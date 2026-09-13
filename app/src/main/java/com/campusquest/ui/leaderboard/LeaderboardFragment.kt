package com.campusquest.ui.leaderboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.campusquest.R
import com.campusquest.databinding.FragmentLeaderboardBinding
import com.campusquest.domain.model.GameLeaderboardEntry

class LeaderboardFragment : Fragment() {

    private var _binding: FragmentLeaderboardBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLeaderboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        binding.rvLeaderboard.layoutManager = LinearLayoutManager(requireContext())
        val entries = listOf(
            GameLeaderboardEntry("u1", "Elena_Rostova", 500, 840, System.currentTimeMillis() - 3600000, 1),
            GameLeaderboardEntry("u2", "Marcus_Vance", 400, 1020, System.currentTimeMillis() - 7200000, 2),
            GameLeaderboardEntry("u3", "Dev_Priya", 300, 1250, System.currentTimeMillis() - 10800000, 3),
            GameLeaderboardEntry("u4", "Kai_Chen", 200, 1480, System.currentTimeMillis() - 14400000, 4)
        )
        binding.rvLeaderboard.adapter = LeaderboardAdapter(entries)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private class LeaderboardAdapter(
        private val items: List<GameLeaderboardEntry>
    ) : RecyclerView.Adapter<LeaderboardAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvRank: TextView = view.findViewById(R.id.tv_rank)
            val tvPlayerName: TextView = view.findViewById(R.id.tv_player_name)
            val tvTimeScore: TextView = view.findViewById(R.id.tv_time_score)
            val tvPoints: TextView = view.findViewById(R.id.tv_points)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_leaderboard_entry, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val entry = items[position]
            holder.tvRank.text = "#${entry.rank}"
            holder.tvPlayerName.text = entry.userName
            val mins = entry.completionTimeSeconds / 60
            val secs = entry.completionTimeSeconds % 60
            holder.tvTimeScore.text = "Completed in ${mins}m ${secs}s"
            holder.tvPoints.text = "${entry.score} pts"
        }

        override fun getItemCount() = items.size
    }
}
