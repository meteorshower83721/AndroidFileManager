package com.example.androidfilemanager

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.io.File

class FileListAdapter(
    private var files: List<File>,
    private val onItemClick: (File) -> Unit
) : RecyclerView.Adapter<FileListAdapter.FileViewHolder>() {

    class FileViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val iconImageView: ImageView = itemView.findViewById(R.id.iconImageView)
        val nameTextView: TextView = itemView.findViewById(R.id.nameTextView)
    }

    // Создание нового ViewHolder (вызывается RecyclerView)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item_file, parent, false)
        return FileViewHolder(view)
    }

    // Привязка данных к ViewHolder (вызывается RecyclerView)
    override fun onBindViewHolder(holder: FileViewHolder, position: Int) {
        val file = files[position]
        holder.nameTextView.text = file.name

        // Установка иконки в зависимости от типа (папка или файл)
        if (file.isDirectory) {
            holder.iconImageView.setImageResource(R.drawable.ic_folder)
        } else {
            holder.iconImageView.setImageResource(R.drawable.ic_file)
        }

        // Установка слушателя кликов
        holder.itemView.setOnClickListener {
            onItemClick(file)
        }
    }

    // Возвращает количество элементов в списке
    override fun getItemCount(): Int = files.size

    // Метод для обновления списка файлов в адаптере
    fun updateData(newFiles: List<File>) {
        files = newFiles.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
        notifyDataSetChanged() // Уведомление для RecyclerView об изменениях (для простоты)
    }
}