package io.github.yoheikikuta.arxivquickcheckerandsaver

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentActivity
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentStatePagerAdapter
import android.support.v4.view.ViewPager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.github.kittinunf.fuel.coroutines.awaitStringResponseResult
import com.github.kittinunf.fuel.httpGet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

class MainActivity : ArxivPapersFragmentActivity() {
    // Just inherit ArxivPapersFragmentActivity
}


abstract class ArxivPapersFragmentActivity : FragmentActivity(), CoroutineScope {
    private lateinit var job: Job
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    // Instantiate a ViewPager by lazy.
    private val pager by lazy { findViewById<ViewPager>(R.id.pager) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        job = Job()

        setContentView(R.layout.activity_screen_slide)

        // Get arXiv RSS information and set the pager adapter which provides the pages to the view paper widget.
        launch {
            val allCategoryItems: MutableList<Item> = mutableListOf()
            val urlList: List<String> = listOf(
                "http://export.arxiv.org/rss/cs.CL",
                "http://export.arxiv.org/rss/cs.CV",
                "http://export.arxiv.org/rss/cs.DS",
                "http://export.arxiv.org/rss/cs.ET",
                "http://export.arxiv.org/rss/cs.GT",
                "http://export.arxiv.org/rss/cs.IR",
                "http://export.arxiv.org/rss/cs.IT",
                "http://export.arxiv.org/rss/cs.MS",
                "http://export.arxiv.org/rss/cs.OS",
                "http://export.arxiv.org/rss/cs.PL",
                "http://export.arxiv.org/rss/cs.SE",
                "http://export.arxiv.org/rss/cs.LG")
            urlList.forEach {
                val (_, _, result) = it.httpGet().awaitStringResponseResult(scope = Dispatchers.IO)
                val data: String = result.get()
                val items: List<Item> = ArxivRSSXmlParser().parse(data) as List<Item>?
                    ?: listOf(Item(title = "NO ENTRY", creator = "", description = "", isNew = true))
                allCategoryItems.addAll(items)
                return@forEach
            }

            val pagerAdapter = ScreenSlidePagerAdapter(supportFragmentManager, allCategoryItems.distinct())
            pager.adapter = pagerAdapter
        }
    }

    /**
     * A simple pager adapter that represents 5 ScreenSlidePageFragment objects, in sequence.
     */
    private inner class ScreenSlidePagerAdapter(fm: FragmentManager, val items: List<Item>) : FragmentStatePagerAdapter(fm) {
        override fun getCount(): Int = items.size

        override fun getItem(position: Int): Fragment = ScreenSlidePageFragment.newInstance(position, items[position])
    }

}

class ScreenSlidePageFragment: Fragment() {

    companion object {
        fun newInstance(position: Int, item: Item): ScreenSlidePageFragment {
            val fragment = ScreenSlidePageFragment()
            val args = Bundle()
            args.putInt("position", position)
            args.putParcelable("item", item)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_screen_slide_page, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val item: Item = arguments!!.get("item") as Item

        val titleTextView = view.findViewById<TextView>(R.id.title)
        val creatorTextView = view.findViewById<TextView>(R.id.creator)
        val descriptionTextView = view.findViewById<TextView>(R.id.description)

        titleTextView.text = item.title
        creatorTextView.text = item.creator
        descriptionTextView.text = item.description
    }
}
