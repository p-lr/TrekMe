package com.peterlaurence.trekme.ui.mapimport

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.MimeTypeMap
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.databinding.FragmentMapImportBinding
import com.peterlaurence.trekme.ui.tools.RecyclerItemClickListener
import com.peterlaurence.trekme.viewmodel.mapimport.MapImportViewModel
import com.peterlaurence.trekme.viewmodel.mapimport.UnzipMapImportedEvent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * A [Fragment] subclass that displays the list of maps archives available for import.
 * Leverages the Storage Access Framework (SAF) to get the list of [DocumentFile]s from a directory
 * selected by the user. The SAF guarantees that the application is granted access to that folder.
 * Using ContentResolver, an InputStream is created from a [DocumentFile] when the user selects it
 * press the extract button. Then, the view-model is involved for the rest of the process.
 *
 * @author peterLaurence on 08/06/16 -- Converted to Kotlin on 18/01/19
 */
@AndroidEntryPoint
class MapImportFragment : Fragment() {
    /* View binding boilerplate */
    private var _binding: FragmentMapImportBinding? = null // backing field
    private val binding get() = _binding!!

    private var mapArchiveAdapter: MapArchiveAdapter? = null
    private var fabEnabled = false
    private lateinit var data: List<MapImportViewModel.ItemData>
    private var itemSelected: MapImportViewModel.ItemData? = null
    private val viewModel: MapImportViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.itemLiveData.observe(this, Observer {
            it?.let { mapArchiveList ->
                this.data = mapArchiveList
                mapArchiveAdapter?.setMapArchiveList(mapArchiveList)
                binding.progressListUris.visibility = View.GONE
                binding.welcomePanel.visibility = View.GONE
                binding.archiveListPanel.visibility = View.VISIBLE
            }
        })

        viewModel.unzipEvents.observe(this, Observer {
            it?.let { event ->
                val itemData = data.firstOrNull { item ->
                    item.id == event.itemId
                } ?: return@let

                mapArchiveAdapter?.setUnzipEventForItem(itemData, event)

                if (event is UnzipMapImportedEvent) {
                    onMapImported()
                }
            }
        })

        setHasOptionsMenu(false)
    }

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentMapImportBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        mapArchiveAdapter = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonImport.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
            startActivityForResult(intent, MAP_IMPORT_CODE)
        }

        val recyclerViewMapImport = binding.recyclerViewMapImport
        recyclerViewMapImport.setHasFixedSize(false)

        val llm = LinearLayoutManager(context)
        recyclerViewMapImport.layoutManager = llm

        mapArchiveAdapter = MapArchiveAdapter()
        recyclerViewMapImport.adapter = mapArchiveAdapter

        recyclerViewMapImport.addOnItemTouchListener(
                RecyclerItemClickListener(this.context,
                        recyclerViewMapImport, object : RecyclerItemClickListener.OnItemClickListener {

                    override fun onItemClick(view: View, position: Int) {
                        binding.fab.activate()
                        singleSelect(position)
                    }

                    override fun onItemLongClick(view: View?, position: Int) {
                        // no-op
                    }
                })
        )

        /* Item decoration : divider */
        val dividerItemDecoration = DividerItemDecoration(
                view.context,
                DividerItemDecoration.VERTICAL
        )
        val divider = view.context.getDrawable(R.drawable.divider)
        if (divider != null) {
            dividerItemDecoration.setDrawable(divider)
        }
        recyclerViewMapImport.addItemDecoration(dividerItemDecoration)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                lifecycleScope.launch {
                    showProgressBar()
                    binding.buttonImport.isEnabled = false
                    val zipDocs = listZipDocs(uri)
                    viewModel.updateUriList(zipDocs)
                }
            }
        }
    }

    private suspend fun listZipDocs(uri: Uri): List<DocumentFile> = withContext(Dispatchers.IO) {
        val context = context ?: return@withContext listOf<DocumentFile>()
        val docFile = DocumentFile.fromTreeUri(context, uri)
                ?: return@withContext listOf<DocumentFile>()
        if (docFile.isDirectory) {
            val zipDocs = docFile.listFiles().filter {
                MimeTypeMap.getSingleton().getExtensionFromMimeType(it.type) == "zip"
            }

            zipDocs
        } else listOf()
    }

    private fun singleSelect(position: Int) {
        /* Update adapter to reflect selection */
        mapArchiveAdapter?.setSelectedPosition(position)
        mapArchiveAdapter?.notifyDataSetChanged()

        /* Keep a reference on the selected archive */
        itemSelected = data[position]
    }

    private fun FloatingActionButton.activate() {
        val fab = binding.fab
        if (!fabEnabled) {
            fab.isEnabled = true
            fab.drawable.mutate().setTint(resources.getColor(R.color.colorWhite, null))
            fab.background.setTint(resources.getColor(R.color.colorAccent, null))

            fab.setOnClickListener {
                itemSelected?.let { item ->
                    viewModel.unarchiveAsync(item)
                }
            }
        }
    }

    private fun onMapImported() {
        val view = view ?: return
        val snackbar = Snackbar.make(view, R.string.snack_msg_show_map_list, Snackbar.LENGTH_LONG)
        snackbar.setAction(R.string.ok_dialog) {
            findNavController().navigate(R.id.mapListFragment)
        }
        snackbar.show()
    }

    private fun showProgressBar() {
        binding.progressListUris.visibility = View.VISIBLE
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putBoolean(CREATE_FROM_SCREEN_ROTATE, true)
    }

    companion object {
        private const val CREATE_FROM_SCREEN_ROTATE = "create"
    }
}

private const val MAP_IMPORT_CODE = 5847
