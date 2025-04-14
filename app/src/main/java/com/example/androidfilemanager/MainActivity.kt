package com.example.androidfilemanager

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.Settings // Для открытия настроек
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.activity.OnBackPressedCallback // Для обработки кнопки "Назад"
import com.example.androidfilemanager.databinding.ActivityMainBinding // Импорт ViewBinding
import java.io.File
import android.util.Log

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: FileListAdapter
    private lateinit var currentDirectory: File

    // Регистрация ActivityResultLauncher для запроса разрешений
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                // Разрешение получено, загружаем корневую директорию
                loadDirectory(Environment.getExternalStorageDirectory())
            } else {
                // Разрешение не получено
                Toast.makeText(this, "Разрешение на чтение хранилища необходимо", Toast.LENGTH_LONG).show()
                // Можно показать диалог с объяснением и кнопкой для перехода в настройки
                // showPermissionRationaleDialog()
                binding.pathTextView.text = "Доступ запрещен"
            }
        }

    // Регистрация обработчика кнопки "Назад"
    private val onBackPressedCallback = object : OnBackPressedCallback(false) { // Изначально выключен
        override fun handleOnBackPressed() {
            // Обрабатываем нажатие кнопки "Назад"
            val parent = currentDirectory.parentFile
            if (parent != null && parent.canRead() && parent != Environment.getExternalStorageDirectory().parentFile) { // Не выходим за пределы внешнего хранилища
                loadDirectory(parent)
            } else {
                // Если родительской папки нет или мы у корня, выходим из приложения (стандартное поведение)
                isEnabled = false // Выключаем наш обработчик
                onBackPressedDispatcher.onBackPressed() // Вызываем стандартный обработчик
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Добавляем наш обработчик кнопки "Назад"
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)

        setupRecyclerView()

        // Проверяем и запрашиваем разрешение
        checkAndRequestPermission()
    }

    // Настройка RecyclerView и адаптера
    private fun setupRecyclerView() {
        adapter = FileListAdapter(emptyList()) { file ->
            // Обработка клика на элемент списка
            if (file.isDirectory) {
                loadDirectory(file)
            } else {
                openFile(file)
            }
        }
        binding.fileRecyclerView.adapter = adapter
        // LayoutManager уже задан в XML, но можно и здесь:
        // binding.fileRecyclerView.layoutManager = LinearLayoutManager(this)
    }

    // Проверка и запрос разрешения на чтение хранилища
    private fun checkAndRequestPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED -> {
                // Разрешение уже есть, загружаем корневую директорию
                loadDirectory(Environment.getExternalStorageDirectory())
            }
            shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE) -> {
                // Показать объяснение пользователю (опционально)
                Toast.makeText(this, "Нужно разрешение для доступа к файлам", Toast.LENGTH_SHORT).show()
                // Запрашиваем разрешение
                requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
            else -> {
                // Запрашиваем разрешение
                requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
    }

    // Загрузка содержимого директории
    private fun loadDirectory(directory: File) {
        if (!directory.canRead()) {
            Toast.makeText(this, "Невозможно прочитать папку", Toast.LENGTH_SHORT).show()
            return
        }

        currentDirectory = directory
        binding.pathTextView.text = directory.absolutePath // Показываем текущий путь

        // Получаем список файлов и папок
        val files = directory.listFiles()?.toList() ?: emptyList()

        // Фильтруем скрытые файлы (начинающиеся с ".") - опционально
        val visibleFiles = files.filter { !it.name.startsWith(".") }

        // Обновляем данные в адаптере
        adapter.updateData(files)

        // Включаем или выключаем наш обработчик кнопки "Назад"
        // Включаем, если мы не в корневой папке
        onBackPressedCallback.isEnabled = directory.absolutePath != Environment.getExternalStorageDirectory().absolutePath
                && directory.parentFile != null // Доп. проверка
    }

    // Открытие файла с помощью системных приложений
    private fun openFile(file: File) {
        val uri: Uri = FileProvider.getUriForFile(
            this,
            "${applicationContext.packageName}.provider", // Должен совпадать с authorities в Manifest
            file
        )

        val intent = Intent(Intent.ACTION_VIEW)
        val mimeType = getMimeType(file) // Определяем MIME-тип

        intent.setDataAndType(uri, mimeType)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) // Важно для FileProvider!

        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, "Нет приложения для открытия файла ${file.name}", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Ошибка при открытии файла: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            // Логирование ошибки
            Log.e("FileManager", "Error opening file $uri", e)
        }
    }

    // Вспомогательная функция для определения MIME-типа файла
    private fun getMimeType(file: File): String? {
        val extension = file.extension.lowercase() // MimeTypeMap работает с расширениями в нижнем регистре
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "*/*" // Возвращаем общий тип, если не найден
    }
}