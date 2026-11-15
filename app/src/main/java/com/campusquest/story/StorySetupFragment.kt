package com.campusquest.story

import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.campusquest.CampusQuestApplication
import com.campusquest.R
import com.campusquest.ui.common.AppProviders


import kotlinx.coroutines.launch

class StorySetupFragment : Fragment() {
    private var card: Bitmap? = null
    private var config: StoryConfigEntity? = null
    private val export = registerForActivityResult(ActivityResultContracts.CreateDocument("image/png")) { uri ->
        if (uri != null) runCatching {
            requireContext().contentResolver.openOutputStream(uri)?.use { card?.compress(Bitmap.CompressFormat.PNG,100,it) }
        }.onFailure { Toast.makeText(context,"Could not save card. Try again.",Toast.LENGTH_LONG).show() }
    }
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View {
        val body = StoryUi.column(requireContext())
        val scroll = ScrollView(requireContext()).apply { addView(body) }
        StoryUi.button(body,"Back") { findNavController().navigateUp() }
        StoryUi.text(body,"Light the Way",28f)
        StoryUi.text(body,"Create the three-chapter walk. Your existing games stay saved. Check these places are safe and install the printed card at chapter 2.")
        val defaults = listOf(7.727295 to 81.696698, 7.727241 to 81.696882, (7.727241 + Math.toDegrees(50.0 / 6371000.0)) to 81.696882)
        val fields = defaults.mapIndexed { i,p ->
            StoryUi.text(body,"${i+1}. ${LightTheWay.names[i]}",21f)
            StoryUi.input(body,"Latitude",p.first.toString()) to StoryUi.input(body,"Longitude",p.second.toString())
        }
        val bearing = StoryUi.input(body,"Lamp direction in degrees (0 = north)","0")
        StoryUi.text(body,"Checkpoint 3 starts about 50 metres north of checkpoint 2. Arrival radius: 15 metres.",14f)
        val message=StoryUi.text(body,"")
        val create=StoryUi.button(body,"Create Light the Way") {}
        create.setOnClickListener {
            val points = fields.map { (lat,lng) -> lat.text.toString().toDoubleOrNull() to lng.text.toString().toDoubleOrNull() }
            val heading = bearing.text.toString().toFloatOrNull()
            if (points.any { it.first == null || it.second == null } || heading == null) { message.text="Enter valid coordinates and a direction.";return@setOnClickListener }
            create.isEnabled=false
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val uid=requireNotNull(AppProviders.authRepository.getCurrentUserId()) { "Please sign in first" }
                    val db=CampusQuestApplication.instance.database
                    val id=LightTheWay.create(db,uid,AppProviders.authRepository.currentUserState.value?.displayName ?: "Explorer",points.map { it.first!! to it.second!! },heading)
                    showCard(body,db.storyDao().config(id)!!)
                    com.campusquest.data.repository.SyncWorker.enqueue(requireContext())
                    message.text="Created and published. Place this card at checkpoint 2 before playing."
                    fields.forEach { it.first.isEnabled=false;it.second.isEnabled=false };bearing.isEnabled=false
                    create.visibility=View.GONE
                } catch (e: Exception) { message.text=e.message ?: "Could not create game";create.isEnabled=true }
            }
        }
        arguments?.getString("gameId")?.let { id ->
            create.visibility=View.GONE
            viewLifecycleOwnerLiveData.observe(this) { owner -> if(owner!=null) owner.lifecycleScope.launch {
                val db=CampusQuestApplication.instance.database
                val game=db.gameDao().getGameById(id)
                if(game?.creatorId == AppProviders.authRepository.getCurrentUserId()) {
                    db.checkpointDao().getCheckpointsForGame(id).forEachIndexed { i,cp -> if(i<fields.size){fields[i].first.setText(cp.lat.toString());fields[i].second.setText(cp.lng.toString())} }
                    db.storyDao().config(id)?.let { bearing.setText(it.targetBearing.toString());showCard(body,it) }
                } else message.text="Only the creator can view the repair card."
                fields.forEach { it.first.isEnabled=false;it.second.isEnabled=false };bearing.isEnabled=false
            } }
        }
        return scroll
    }
    private fun showCard(body: LinearLayout, c: StoryConfigEntity) {
        config=c
        card=RepairCardArtwork.render(c)
        body.addView(ImageView(requireContext()).apply {setImageBitmap(card);adjustViewBounds=true;contentDescription="Printable repair card. Backup code ${c.backupCode}"},LinearLayout.LayoutParams(-1,-2))
        StoryUi.button(body,"Save printable card") { export.launch("Light-the-Way-Repair-Card.png") }
        StoryUi.button(body,"Open game") { findNavController().navigate(R.id.gameDetailFragment,Bundle().apply {putString("gameId",c.gameId)}) }
    }
}


