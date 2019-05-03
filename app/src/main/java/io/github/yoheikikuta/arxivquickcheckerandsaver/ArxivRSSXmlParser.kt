package io.github.yoheikikuta.arxivquickcheckerandsaver

import android.content.ClipData
import android.os.Parcelable
import android.text.Html
import android.util.Xml
import kotlinx.android.parcel.Parcelize
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.IOException
import java.io.StringReader

@Parcelize
data class Item(val title: String, val creator: String, val description: String, val isNew: Boolean): Parcelable

class ArxivRSSXmlParser {

    val ns: String? = null

    @Throws(XmlPullParserException::class, IOException::class)
    fun parse(inputString: String): List<*> {
        val parser: XmlPullParser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(StringReader(inputString))
        parser.nextTag()
        return readFeed(parser)
    }

    @Throws(XmlPullParserException::class, IOException::class)
    fun readFeed(parser: XmlPullParser): MutableList<Item> {
        val entries = mutableListOf<Item>()

        parser.require(XmlPullParser.START_TAG, ns, "rdf:RDF")
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) {
                continue
            }
            // Starts by looking for the item tag
            if (parser.name == "item") {
                readItem(parser).let { if (it.isNew) {entries.add(it)} }
            } else {
                skip(parser)
            }
        }
        return entries
    }

    // Parses the contents of an item. If it encounters a title, dc:creator, or description tag, hands them off
    // to their respective "read" methods for processing. Otherwise, skips the tag.
    @Throws(XmlPullParserException::class, IOException::class)
    fun readItem(parser: XmlPullParser): Item {
        parser.require(XmlPullParser.START_TAG, ns, "item")
        var title = ""
        var creator = ""
        var description = ""
        var isNew = true
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) {
                continue
            }
            when (parser.name) {
                "title" -> title = readTitle(parser)
                "dc:creator" -> creator = readCreator(parser)
                "description" -> description = readDescription(parser)
                else -> skip(parser)
            }
        }

        // Postprocessing: check whether the paper is new and remove (arXiv:XXXX.XXXX) information in the title.
        isNew = !Regex("UPDATED").containsMatchIn(title)
        title = title.replace(" \\(arXiv:.*\\)".toRegex(),"")

        return Item(title, creator, description, isNew)
    }

    // Processes title tags in the feed.
    @Throws(IOException::class, XmlPullParserException::class)
    fun readTitle(parser: XmlPullParser): String {
        parser.require(XmlPullParser.START_TAG, ns, "title")
        var title = readText(parser)
        parser.require(XmlPullParser.END_TAG, ns, "title")
        return title
    }

    // Processes creator tags in the feed.
    @Throws(IOException::class, XmlPullParserException::class)
    fun readCreator(parser: XmlPullParser): String {
        parser.require(XmlPullParser.START_TAG, ns, "dc:creator")
        val creator = Html.fromHtml(readText(parser)).toString()
        parser.require(XmlPullParser.END_TAG, ns, "dc:creator")
        return creator
    }

    // Processes description tags in the feed.
    @Throws(IOException::class, XmlPullParserException::class)
    fun readDescription(parser: XmlPullParser): String {
        parser.require(XmlPullParser.START_TAG, ns, "description")
        val description = Html.fromHtml(readText(parser)).toString()
        parser.require(XmlPullParser.END_TAG, ns, "description")
        return description
    }

    // Extracts their text values.
    @Throws(IOException::class, XmlPullParserException::class)
    fun readText(parser: XmlPullParser): String {
        var result = ""
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.text
            parser.nextTag()
        }
        return result
    }

    @Throws(XmlPullParserException::class, IOException::class)
    fun skip(parser: XmlPullParser) {
        if (parser.eventType != XmlPullParser.START_TAG) {
            throw IllegalStateException()
        }
        var depth = 1
        while (depth != 0) {
            when (parser.next()) {
                XmlPullParser.END_TAG -> depth--
                XmlPullParser.START_TAG -> depth++
            }
        }
    }

}
