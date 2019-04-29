package io.github.yoheikikuta.arxivquickcheckerandsaver

import android.app.DownloadManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Xml
import android.widget.TextView
import com.github.kittinunf.fuel.httpGet
import java.net.URL
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.result.Result
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.IOException
import java.io.InputStream
import java.io.StringReader
import java.net.HttpURLConnection

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        data class Item(val title: String?, val creator: String?, val description: String?)

        val ns: String? = null

        class ArxivRSSXmlParser {

            @Throws(XmlPullParserException::class, IOException::class)
            fun parse(inputString: String): List<*> {
                val parser: XmlPullParser = Xml.newPullParser()
                parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
                parser.setInput(StringReader(inputString))
                parser.nextTag()
                return readFeed(parser)
            }
//            fun parse(inputStream: InputStream): List<*> {
//                inputStream.use { inputStream ->
//                    val parser: XmlPullParser = Xml.newPullParser()
//                    parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
//                    parser.setInput(inputStream, null)
//                    parser.nextTag()
//                    return readFeed(parser)
//                }
//            }

            @Throws(XmlPullParserException::class, IOException::class)
            fun readFeed(parser: XmlPullParser): List<Item> {
                val entries = mutableListOf<Item>()

                parser.require(XmlPullParser.START_TAG, ns, "rdf:RDF")
                while (parser.next() != XmlPullParser.END_TAG) {
                    if (parser.eventType != XmlPullParser.START_TAG) {
                        continue
                    }
                    // Starts by looking for the item tag
                    if (parser.name == "item") {
                        entries.add(readItem(parser))
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
                var title: String? = null
                var creator: String? = null
                var description: String? = null
                while (parser.next() != XmlPullParser.END_TAG) {
                    if (parser.eventType != XmlPullParser.START_TAG) {
                        continue
                    }
                    when (parser.name) {
                        "title" -> title = readTitle(parser)
                        "creator" -> creator = readCreator(parser)
                        "description" -> description = readDescription(parser)
                        else -> skip(parser)
                    }
                }
                return Item(title, creator, description)
            }

            // Processes title tags in the feed.
            @Throws(IOException::class, XmlPullParserException::class)
            fun readTitle(parser: XmlPullParser): String {
                parser.require(XmlPullParser.START_TAG, ns, "title")
                val title = readText(parser)
                parser.require(XmlPullParser.END_TAG, ns, "title")
                return title
            }

            // Processes creator tags in the feed.
            @Throws(IOException::class, XmlPullParserException::class)
            fun readCreator(parser: XmlPullParser): String {
//                var creator = ""
                parser.require(XmlPullParser.START_TAG, ns, "dc:creator")
                val creator = readText(parser)
//                val tag = parser.name
//                val relType = parser.getAttributeValue(null, "rel")
//                if (tag == "link") {
//                    if (relType == "alternate") {
//                        link = parser.getAttributeValue(null, "href")
//                        parser.nextTag()
//                    }
//                }
                parser.require(XmlPullParser.END_TAG, ns, "dc:creator")
                return creator
            }

            // Processes description tags in the feed.
            @Throws(IOException::class, XmlPullParserException::class)
            fun readDescription(parser: XmlPullParser): String {
                parser.require(XmlPullParser.START_TAG, ns, "description")
                val description = readText(parser)
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

//        @Throws(IOException::class)
//        fun downloadUrl(urlString: String): InputStream? {
//            val url = URL(urlString)
//            return (url.openConnection() as? HttpURLConnection)?.run {
//                readTimeout = 10000
//                connectTimeout = 15000
//                requestMethod = "GET"
//                doInput = true
//                // Starts the query
//                connect()
//                inputStream
//            }
//        }
//
//        val items: List<Item> = downloadUrl("http://export.arxiv.org/rss/cs.CV")?.use { stream ->
//            // Instantiate the parser
//            ArxivRSSXmlParser().parse(stream)
//        } as List<Item>? ?: emptyList()


        val textView = findViewById<TextView>(R.id.text)

        "http://export.arxiv.org/rss/cs.CV".httpGet().responseString { request, response, result ->
            when (result) {
                is Result.Failure -> {
                    val ex = result.getException()
                }
                is Result.Success -> {
                    val data = result.get()
                    val items: List<Item>? = ArxivRSSXmlParser().parse(data) as List<Item>?
//                    textView.text = "Response is: ${data.substring(0, 500)}"
                    textView.text = items!![0].description
                }
            }
        }
    }

}
