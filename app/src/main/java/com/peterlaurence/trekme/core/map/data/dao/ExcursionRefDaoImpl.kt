package com.peterlaurence.trekme.core.map.data.dao

import com.peterlaurence.trekme.core.map.data.mappers.toData
import com.peterlaurence.trekme.core.map.data.mappers.toDomain
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
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import java.io.File

class ExcursionRefDaoImpl(
    private val ioDispatcher: CoroutineDispatcher,
    private val json: Json
) : ExcursionRefDao {
    override suspend fun importExcursionRefs(map: Map) = withContext(ioDispatcher) {
        val directory = (map as? MapFileBased)?.folder ?: return@withContext
        val refsDir = File(directory, excursionRefsDir)
        runCatching {
            if (!refsDir.exists()) return@withContext
            val refFiles = refsDir.listFiles { it: File ->
                it.isFile && it.nameWithoutExtension.toIntOrNull() != null && it.name.endsWith(".json")
            } ?: emptyArray()

            val refs = refFiles.map {
                json.decodeFromString<ExcursionRefKtx>(FileUtils.getStringFromFile(it)) to it
            }

            map.excursionRefs.update { refs.map { it.toDomain() } }
        }
        Unit
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

    override suspend fun removeExcursionRef(ref: ExcursionRef) = withContext(ioDispatcher) {
        val refFileBased = (ref as? ExcursionRefFileBased) ?: return@withContext

        runCatching {
            refFileBased.file.delete()
        }
        Unit
    }
}
