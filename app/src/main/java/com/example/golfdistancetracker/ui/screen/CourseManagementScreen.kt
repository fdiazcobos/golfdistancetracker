package com.example.golfdistancetracker.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.golfdistancetracker.data.entity.Course
import com.example.golfdistancetracker.ui.viewmodel.CourseViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseManagementScreen(viewModel: CourseViewModel = hiltViewModel()) {
    val courses by viewModel.courses.collectAsState()
    var courseToEdit by remember { mutableStateOf<Course?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedCourseForHoles by remember { mutableStateOf<Course?>(null) }

    Scaffold(
        topBar = { 
            TopAppBar(
                title = { Text(selectedCourseForHoles?.name ?: "My Golf Courses") },
                navigationIcon = {
                    if (selectedCourseForHoles != null) {
                        IconButton(onClick = { selectedCourseForHoles = null }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                }
            ) 
        },
        floatingActionButton = {
            if (selectedCourseForHoles == null) {
                FloatingActionButton(onClick = { showAddDialog = true }, containerColor = MaterialTheme.colorScheme.primary) {
                    Icon(Icons.Default.Add, contentDescription = "Add Course")
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (selectedCourseForHoles == null) {
                if (courses.isEmpty()) {
                    EmptyCoursesView()
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 80.dp)) {
                        items(courses) { course ->
                            CourseItem(
                                course, 
                                onClick = { selectedCourseForHoles = course },
                                onEdit = { courseToEdit = course },
                                onDelete = { viewModel.deleteCourse(course) }
                            )
                        }
                    }
                }
            } else {
                val holes by viewModel.getHoles(selectedCourseForHoles!!.id).collectAsState(emptyList())
                HoleList(holes) { hole -> viewModel.updateHole(hole) }
            }
        }

        if (showAddDialog) {
            CourseDialog(
                onDismiss = { showAddDialog = false },
                onSave = { name, loc, holes ->
                    viewModel.addCourse(name, loc, holes)
                    showAddDialog = false
                }
            )
        }

        if (courseToEdit != null) {
            CourseDialog(
                initialCourse = courseToEdit,
                onDismiss = { courseToEdit = null },
                onSave = { name, loc, holes ->
                    viewModel.updateCourse(courseToEdit!!.copy(name = name, location = loc, numberOfHoles = holes))
                    courseToEdit = null
                }
            )
        }
    }
}

@Composable
fun EmptyCoursesView() {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AsyncImage(
            model = "https://images.unsplash.com/photo-1593111774240-d529f12cf4bb?q=80&w=600&auto=format&fit=crop",
            contentDescription = null,
            modifier = Modifier.size(200.dp).clip(CircleShape),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text("No courses yet", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
            "Add the courses where you play to track your performance by hole.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            color = MaterialTheme.colorScheme.secondary
        )
    }
}

@Composable
fun CourseItem(course: Course, onClick: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        onClick = onClick
    ) {
        Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(course.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.secondary)
                    Spacer(Modifier.width(4.dp))
                    Text("${course.numberOfHoles} Holes • ${course.location ?: "Unknown"}", style = MaterialTheme.typography.bodyMedium)
                }
            }
            Row {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
fun HoleList(holes: List<com.example.golfdistancetracker.data.entity.Hole>, onHoleUpdate: (com.example.golfdistancetracker.data.entity.Hole) -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(8.dp)) {
        items(holes) { hole ->
            var showDetailDialog by remember { mutableStateOf(false) }
            
            ElevatedCard(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                onClick = { showDetailDialog = true }
            ) {
                ListItem(
                    headlineContent = { Text("Hole ${hole.holeNumber}", fontWeight = FontWeight.Bold) },
                    supportingContent = { 
                        Text(
                            hole.notes ?: "Tap to add strategy notes...", 
                            maxLines = 2,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        ) 
                    },
                    leadingContent = {
                        Box(
                            modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("${hole.holeNumber}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    },
                    trailingContent = {
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Par ${hole.par}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.LightGray)
                        }
                    }
                )
            }

            if (showDetailDialog) {
                HoleDetailDialog(
                    hole = hole,
                    onDismiss = { showDetailDialog = false },
                    onSave = { updatedHole ->
                        onHoleUpdate(updatedHole)
                        showDetailDialog = false
                    }
                )
            }
        }
    }
}

@Composable
fun HoleDetailDialog(hole: com.example.golfdistancetracker.data.entity.Hole, onDismiss: () -> Unit, onSave: (com.example.golfdistancetracker.data.entity.Hole) -> Unit) {
    var par by remember { mutableIntStateOf(hole.par) }
    var notes by remember { mutableStateOf(hole.notes ?: "") }
    var dist by remember { mutableStateOf(hole.distance?.toString() ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Hole ${hole.holeNumber} Details", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("Par:", fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { if(par > 3) par-- }) { Icon(Icons.Default.Remove, null) }
                        Text("$par", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        IconButton(onClick = { if(par < 6) par++ }) { Icon(Icons.Default.Add, null) }
                    }
                }
                
                OutlinedTextField(
                    value = dist,
                    onValueChange = { dist = it },
                    label = { Text("Typical Distance") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Strategy Notes") },
                    placeholder = { Text("e.g. Aim left of the bunker") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
            }
        },
        confirmButton = {
            Button(onClick = { 
                onSave(hole.copy(par = par, notes = notes.ifEmpty { null }, distance = dist.toIntOrNull())) 
            }) { Text("Save Details") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun CourseDialog(initialCourse: Course? = null, onDismiss: () -> Unit, onSave: (String, String?, Int) -> Unit) {
    var name by remember { mutableStateOf(initialCourse?.name ?: "") }
    var location by remember { mutableStateOf(initialCourse?.location ?: "") }
    var holes by remember { mutableIntStateOf(initialCourse?.numberOfHoles ?: 18) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialCourse == null) "Add New Course" else "Edit Course Info", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Course Name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = location, onValueChange = { location = it }, label = { Text("Location") }, modifier = Modifier.fillMaxWidth())
                
                Text("Number of Holes", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = holes == 9, onClick = { holes = 9 })
                        Text("9 Holes")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = holes == 18, onClick = { holes = 18 })
                        Text("18 Holes")
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(name, location.ifEmpty { null }, holes) }) {
                Text(if (initialCourse == null) "Create" else "Update")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
