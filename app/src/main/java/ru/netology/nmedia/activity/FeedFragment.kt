package ru.netology.nmedia.activity

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import ru.netology.nmedia.R
import ru.netology.nmedia.adapter.OnInteractionListener
import ru.netology.nmedia.adapter.PostsAdapter
import ru.netology.nmedia.databinding.FragmentFeedBinding
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.viewmodel.PostViewModel

class FeedFragment : Fragment() {

    private val viewModel: PostViewModel by activityViewModels()
    private var pendingScrollToTop = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentFeedBinding.inflate(inflater, container, false)

        val adapter = PostsAdapter(object : OnInteractionListener {
            override fun onEdit(post: Post) {
                viewModel.edit(post)
            }

            override fun onLike(post: Post) {
                viewModel.likeById(post.id)
            }

            override fun onRemove(post: Post) {
                viewModel.removeById(post.id)
            }

            override fun onShare(post: Post) {
                val intent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, post.content)
                    type = "text/plain"
                }

                val shareIntent =
                    Intent.createChooser(intent, getString(R.string.chooser_share_post))
                startActivity(shareIntent)
            }
        })
        binding.list.adapter = adapter
        viewModel.dataState.observe(viewLifecycleOwner) { state ->
            binding.progress.isVisible = state.loading
            binding.swiperefresh.isRefreshing = state.refreshing
            if (state.error) {
                Snackbar.make(binding.root, R.string.error_loading, Snackbar.LENGTH_LONG)
                    .setAction(R.string.retry_loading) { viewModel.loadPosts() }
                    .show()
            }
        }
        viewModel.data.observe(viewLifecycleOwner) { state ->
            adapter.submitList(state.posts) {
                if (pendingScrollToTop && state.posts.isNotEmpty()) {
                    binding.list.smoothScrollToPosition(0)
                    pendingScrollToTop = false
                }
            }
            binding.emptyText.isVisible = state.empty
        }

        viewModel.hiddenCount.observe(viewLifecycleOwner) { count ->
            val shouldShow = count > 0
            if (binding.newPosts.isVisible == shouldShow) return@observe
            if (shouldShow) {
                binding.newPosts.alpha = 0f
                binding.newPosts.translationY = -24f
                binding.newPosts.isVisible = true
                binding.newPosts.animate()
                    .alpha(1f).translationY(0f).setDuration(200).start()
            } else {
                binding.newPosts.animate()
                    .alpha(0f).translationY(-24f).setDuration(200)
                    .withEndAction { binding.newPosts.isVisible = false }
                    .start()
            }
        }
        viewModel.newerCount.observe(viewLifecycleOwner) {
            println(it)
        }

        viewModel.scrollToTop.observe(viewLifecycleOwner) {
            pendingScrollToTop = true
        }

        binding.swiperefresh.setOnRefreshListener {
            viewModel.refreshPosts()
        }

        binding.fab.setOnClickListener {
            findNavController().navigate(R.id.action_feedFragment_to_newPostFragment)
        }

        binding.newPosts.setOnClickListener {
            viewModel.showNewPosts()
        }

        return binding.root
    }
}
