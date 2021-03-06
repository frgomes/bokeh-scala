package io.continuum.bokeh

import java.io.File
import java.nio.file.Files
import java.nio.charset.StandardCharsets.UTF_8

import java.awt.Desktop

import scala.collection.mutable.ListBuffer

import scalatags.Text.short._
import scalatags.Text.tags2.title

class Document(objs: Component*) {
    private val objects = ListBuffer[Component](objs: _*)

    def add(objs: Component*) {
        objects ++= objs
    }

    def fragment(resources: Resources): HTMLFragment = HTMLFragmentWriter(objects.toList, resources).write()
    def fragment(): HTMLFragment = fragment(Res.default)

    def autoload(resources: Resources): HTMLFragment = new AutoloadWriter(objects.toList, resources).write()
    def autoload(): HTMLFragment = autoload(Res.default)

    def save(file: File, resources: Resources): HTMLFile = HTMLFileWriter(objects.toList, resources).write(file)
    def save(file: File): HTMLFile = save(file, Res.default)

    def save(path: String, resources: Resources): HTMLFile = save(new File(path), resources)
    def save(path: String): HTMLFile = save(new File(path))
}

class HTMLFragment(val html: Seq[Tag], val embed: Seq[Tag], val styles: Seq[Tag], val scripts: Seq[Tag]) {
    def head: Seq[Tag] = styles ++ scripts
    def body: Seq[Tag] = html ++ embed

    override def toString = {
        val h = head.map(_.pretty).mkString("\n")
        val b = body.map(_.pretty).mkString("\n")
        s"$h\n\n$b"
    }
}

object HTMLFragmentWriter {
    def apply(obj: Component): HTMLFragmentWriter = apply(obj, Res.default)

    def apply(obj: Component, resources: Resources): HTMLFragmentWriter = apply(obj :: Nil, resources)

    def apply(objs: List[Component]): HTMLFragmentWriter = apply(objs, Res.default)

    def apply(objs: List[Component], resources: Resources): HTMLFragmentWriter = new HTMLFragmentWriter(objs, resources)
}

class HTMLFragmentWriter(objs: List[Component], resources: Resources) {
    def write(): HTMLFragment = {
        var bundle = resources.bundle(all_objs)
        new HTMLFragment(divs, Seq(embed.asScript), bundle.styles, bundle.scripts)
    }

    lazy val all_objs: List[Model] = Model.collect(objs)

    case class ModelRepr(id: String, `type`: String, attributes: Js.Obj)
    case class Roots(root_ids: List[String], references: List[ModelRepr])
    case class Doc(roots: Roots, title: String, version: String)
    case class RenderItem(docid: String, elementid: String, modelid: Option[String])

    case class Spec(docs_json: Map[String, Doc], render_items: List[RenderItem])

    protected def docTitle: String = "Bokeh Application"

    protected def modelRepr(obj: Model): ModelRepr = {
        val Ref(id, tpe) = obj.getRef
        ModelRepr(id, tpe, obj.fieldsToJson(false))
    }

    protected lazy val spec: Spec = {
        val roots = Roots(objs.map(_.id), all_objs.map(modelRepr))
        var doc = Doc(roots, docTitle, Version.toString)
        val docid = IdGenerator.next()
        val elementid = IdGenerator.next()
        val render_item = RenderItem(docid, elementid, None)
        Spec(Map(docid -> doc), render_item :: Nil)
    }

    protected def divs: Seq[Tag] = {
        spec.render_items.map(item => div(*.`class`:="plotdiv", *.id:=item.elementid))
    }

    protected def embed: String = {
        val code = s"""
            |var docs_json = ${resources.stringify(spec.docs_json)};
            |var render_items = ${resources.stringify(spec.render_items)};
            |
            |Bokeh.embed.embed_items(docs_json, render_items);
            """
        code.stripMargin.trim
    }
}

class AutoloadWriter(objs: List[Component], resources: Resources) extends HTMLFragmentWriter(objs, resources) {
    override def write(): HTMLFragment = {
        var bundle = resources.bundle(all_objs)

        val jsUrls  = bundle.jsUrls.map(_.toString)
        val jsRaw   = bundle.jsRaw ++ Seq(embed)
        val cssUrls = bundle.cssUrls.map(_.toString)
        val cssRaw  = bundle.cssRaw.map(css => Json.write(css))

        val autoload = Autoload.template(jsUrls, jsRaw, cssUrls, cssRaw)

        new HTMLFragment(divs, Seq(autoload.asScript), Nil, Nil)
    }
}

class HTMLFile(val file: File) {
    def url: String = {
        val uri = file.toURI
        s"${uri.getScheme}://${uri.getSchemeSpecificPart}"
    }

    def view() {
        if (Desktop.isDesktopSupported && Desktop.getDesktop.isSupported(Desktop.Action.BROWSE))
            Desktop.getDesktop.browse(file.toURI)
    }
}

object HTMLFileWriter {
    def apply(obj: Component): HTMLFileWriter = apply(obj, Res.default)

    def apply(obj: Component, resources: Resources): HTMLFileWriter = apply(obj :: Nil, resources)

    def apply(objs: List[Component]): HTMLFileWriter = apply(objs, Res.default)

    def apply(objs: List[Component], resources: Resources): HTMLFileWriter = new HTMLFileWriter(objs, resources)
}

class HTMLFileWriter(objs: List[Component], resources: Resources) extends HTMLFragmentWriter(objs, resources) {
    def write(file: File): HTMLFile = {
        val html = stringify(renderFile(write()))
        Files.write(file.toPath, html.getBytes(UTF_8))
        new HTMLFile(file)
    }

    protected def stringify(html: Tag) = {
        val doctype = "<!DOCTYPE html>"
        s"$doctype\n${html.pretty}"
    }

    protected def renderTitle: Tag = {
        val text = spec
            .render_items
            .headOption
            .flatMap { item => spec.docs_json.get(item.docid) }
            .map(_.title)
            .getOrElse(docTitle)
        title(text)
    }

    protected def renderFile(fragment: HTMLFragment): Tag = {
        html(
            head(
                meta(*.charset:="utf-8"),
                renderTitle,
                fragment.head
            ),
            body(fragment.html, fragment.embed)
        )
    }
}
