package io.github.yoheikikuta.arxivquickcheckerandsaver

import android.app.DownloadManager
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentActivity
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentStatePagerAdapter
import android.support.v4.view.ViewPager
import android.text.Html
import android.util.Xml
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
                        "dc:creator" -> creator = readCreator(parser)
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

        val textViewTitle = findViewById<TextView>(R.id.title)
        val textViewCreator = findViewById<TextView>(R.id.creator)
        val textViewDescription = findViewById<TextView>(R.id.description)

        "http://export.arxiv.org/rss/cs.CV".httpGet().responseString { request, response, result ->
            when (result) {
                is Result.Failure -> {
                    val ex = result.getException()
                }
                is Result.Success -> {
                    val data = result.get()
                    val items: List<Item>? = ArxivRSSXmlParser().parse(data) as List<Item>?
                    textViewTitle.text = items!![1].title
                    textViewCreator.text = items!![1].creator
                    textViewDescription.text = items!![1].description

                    val secondIntent = Intent(this, ScreenSlidePagerActivity::class.java)
                    startActivity(secondIntent)
                }
            }
        }

    }

}

data class Item(val title: String?, val creator: String?, val description: String?)

class ScreenSlidePageFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_screen_slide_page, container, false)
}

//class ScreenSlidePagerActivity(items:List<Item>?) : FragmentActivity() {
class ScreenSlidePagerActivity() : FragmentActivity() {

    /**
     * The pager widget, which handles animation and allows swiping horizontally to access previous
     * and next wizard steps.
     */
    private lateinit var mPager: ViewPager
//    private val numItems: Int? = items?.size

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_screen_slide)

        // Instantiate a ViewPager and a PagerAdapter.
        mPager = findViewById(R.id.pager)

        // The pager adapter, which provides the pages to the view pager widget.
        val pagerAdapter = ScreenSlidePagerAdapter(supportFragmentManager)
        mPager.adapter = pagerAdapter
    }

    override fun onBackPressed() {
        if (mPager.currentItem == 0) {
            // If the user is currently looking at the first step, allow the system to handle the
            // Back button. This calls finish() on this activity and pops the back stack.
            super.onBackPressed()
        } else {
            // Otherwise, select the previous step.
            mPager.currentItem = mPager.currentItem - 1
        }
    }

    /**
     * A simple pager adapter that represents 5 ScreenSlidePageFragment objects, in
     * sequence.
     */
    private inner class ScreenSlidePagerAdapter(fm: FragmentManager) : FragmentStatePagerAdapter(fm) {
//        override fun getCount(): Int = numItems!!
        override fun getCount(): Int = 5

        override fun getItem(position: Int): Fragment = ScreenSlidePageFragment()
    }
}
