package com.peterlaurence.trekme.core.map.data.dao

import com.peterlaurence.trekme.core.excursion.domain.model.Excursion
import com.peterlaurence.trekme.core.map.data.mappers.makeDomainExcursionRef
import com.peterlaurence.trekme.core.map.data.mappers.toData
import com.peterlaurence.trekme.core.map.data.models.ExcursionRefFileBased
import com.peterlaurence.trekme.core.map.data.models.ExcursionRefKtx
import com.peterlaurence.trekme.core.map.data.models.MapFileBased
import com.peterlaurence.trekme.core.map.domain.dao.ExcursionRefDao
import com.peterlaurence.trekme.core.map.domain.models.ExcursionRef
import com.peterlaurence.trekme.core.map.domain.models.Map
import com.peterlaurence.trekme.util.FileUtils
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import java.io.File

class ExcursionRefDaoImpl(
    private val ioDispatcher: CoroutineDispatcher,
    private val json: Json
) : ExcursionRefDao {
    override suspend fun importExcursionRefs(
        map: Map,
        excursionProvider: suspend (String) -> Excursion?
    ) = withContext(ioDispatcher) {
        val directory = (map as? MapFileBased)?.folder ?: return@withContext
        val refsDir = File(directory, excursionRefsDir)
        runCatching {
            if (!refsDir.exists()) return@withContext
            val refFiles = refsDir.listFiles { it: File ->
                it.isFile && it.name.endsWith(".json")
            } ?: emptyArray()

            val refs = refFiles.map {
                json.decodeFromString<ExcursionRefKtx>(FileUtils.getStringFromFile(it)) to it
            }.distinctBy {
                /* Protect against copy-paste of an excursion ref file */
                it.first.id
            }

            map.excursionRefs.update {
                refs.mapNotNull { (refKtx, file) ->
                    val excursion = excursionProvider(refKtx.id) ?: return@mapNotNull null
                    makeDomainExcursionRef(refKtx, file, excursion)
                }
            }
        }
        Unit
    }

    override suspend fun createExcursionRef(map: Map, excursion: Excursion): ExcursionRef? = withContext(ioDispatcher) {
        val existing = map.excursionRefs.value.firstOrNull {
            it.id == excursion.id
        }
        /* If the reference already exists, fast return it */
        if (existing != null) return@withContext existing

        val directory = (map as? MapFileBased)?.folder ?: return@withContext null
        val refsDir = File(directory, excursionRefsDir).also {
            val success = if (!it.exists()) it.mkdir() else true
            if (!success) return@withContext null
        }

        runCatching {
            val newRefFile = File(refsDir, "${excursion.id}.json")
            val data = ExcursionRefKtx(id = excursion.id, visible = true)
            val st = json.encodeToString(data)
            FileUtils.writeToFile(st, newRefFile)

            val newRef = makeDomainExcursionRef(data, newRefFile, excursion)
            map.excursionRefs.update {
                it + newRef
            }
            newRef
        }.getOrNull()
    }

    override suspend fun saveExcursionRef(map: Map, ref: ExcursionRef) = withContext(ioDispatcher) {
        val refFileBased = (ref as? ExcursionRefFileBased) ?: return@withContext

        runCatching {
            if (!refFileBased.file.exists()) return@withContext
            val content = json.encodeToString(refFileBased.toData())
            FileUtils.writeToFile(content, refFileBased.file)
        }
        Unit
    }

    override suspend fun removeExcursionRef(map: Map, ref: ExcursionRef) = withContext(ioDispatcher) {
        val refFileBased = (ref as? ExcursionRefFileBased) ?: return@withContext

        runCatching {
            if (refFileBased.file.delete()) {
                map.excursionRefs.update {
                    it.filterNot { r -> r.id == ref.id }
                }
            }
        }
        Unit
    }

    override suspend fun removeExcursionRef(map: Map, excursionRefId: String) {
        val ref = map.excursionRefs.value.firstOrNull { it.id == excursionRefId }

        if (ref != null) {
            removeExcursionRef(map, ref)
        }
    }
}
