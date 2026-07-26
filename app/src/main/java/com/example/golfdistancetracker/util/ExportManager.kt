package com.example.golfdistancetracker.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.golfdistancetracker.data.dao.ClubDao
import com.example.golfdistancetracker.data.dao.ShotDao
import kotlinx.coroutines.flow.first
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class ExportManager(private val context: Context, private val shotDao: ShotDao, private val clubDao: ClubDao) {

    suspend fun exportToCsv() {
        val shots = shotDao.getAllShots().first()
        val clubs = clubDao.getAllClubs().first().associateBy { it.id }
        
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        val csvHeader = "Date,Club,Type,Distance,Deviation,Quality,IsMishit,Notes\n"
        val csvContent = StringBuilder(csvHeader)
        
        shots.forEach { shot ->
            val club = clubs[shot.clubId]
            csvContent.append("${sdf.format(Date(shot.timestamp))},")
            csvContent.append("${club?.name ?: "Unknown"},")
            csvContent.append("${shot.shotType.name},")
            csvContent.append("${shot.distance ?: ""},")
            csvContent.append("${shot.lateralDeviation ?: shot.deviation ?: ""},")
            csvContent.append("${shot.quality ?: ""},")
            csvContent.append("${shot.isMishit},")
            csvContent.append("${(shot.notes ?: "").replace(",", " ")}\n")
        }

        val fileName = "Golf_Stats_${System.currentTimeMillis()}.csv"
        val file = File(context.cacheDir, fileName)
        file.writeText(csvContent.toString())

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        context.startActivity(Intent.createChooser(intent, "Export Golf Stats").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }
}
