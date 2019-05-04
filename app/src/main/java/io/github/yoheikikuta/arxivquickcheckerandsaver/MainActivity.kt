package io.github.yoheikikuta.arxivquickcheckerandsaver

import android.icu.lang.UCharacter.GraphemeClusterBreak.T
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
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.result.Result

class MainActivity : FragmentActivity() {

    /**
     * The pager widget, which handles animation and allows swiping horizontally to access previous
     * and next wizard steps.
     */

    // Instantiate a ViewPager by lazy.
    private val pager by lazy { findViewById<ViewPager>(R.id.pager) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_screen_slide)

        // Get arXiv RSS information and set the pager adapter which provides the pages to the view paper widget.
        "http://export.arxiv.org/rss/cs.CV".httpGet().responseString { _, _, result ->
            when (result) {
                is Result.Failure -> {
                    val ex = result.getException()
                }
                is Result.Success -> {
                    val data = result.get()
                    val items: List<Item> = ArxivRSSXmlParser().parse(data) as List<Item>?
                        ?: listOf(Item(title = "NO ENTRY", creator = "", description = "", isNew = true))
                    val pagerAdapter = ScreenSlidePagerAdapter(supportFragmentManager, items)
                    pager.adapter = pagerAdapter
                }
            }
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
