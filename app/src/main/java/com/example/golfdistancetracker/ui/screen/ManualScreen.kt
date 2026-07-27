package com.example.golfdistancetracker.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.golfdistancetracker.R
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualScreen(onBack: () -> Unit) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Swing", "Corto", "Distancias", "Estrategia", "Etiqueta", "Rutina", "Doctor", "Glosario", "Reglamento")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Librito de Golf", fontWeight = FontWeight.ExtraBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                edgePadding = 16.dp,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            when (selectedTab) {
                0 -> SwingSection()
                1 -> ShortGameSection()
                2 -> DistanceSection()
                3 -> StrategySection()
                4 -> EtiquetteSection()
                5 -> RoutineSection()
                6 -> DoctorSection()
                7 -> GlossarySection()
                8 -> RegulationSection()
            }
        }
    }
}

@Composable
fun SwingSection() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { ManualHeader("1. El Agarre (Grip)") }
        item {
            ManualCard {
                Text("Define la cara del palo en el impacto.", fontWeight = FontWeight.Bold)
                BulletPoint("Presión (3-4/10): Firmé pero sin tensión. Como sostener un tubo de pasta abierto.")
                BulletPoint("Integración: Usá agarre Interlocking o Overlapping para trabajar como una unidad.")
                BulletPoint("Mano delantera: La varilla cruza en diagonal desde la base del índice hasta la almohadilla del meñique.")
                BulletPoint("Mano trasera: Asienta en la hendidura del pulgar izquierdo.")
            }
        }

        item { ManualHeader("2. Postura y Alineación") }
        item {
            ManualCard {
                Text("Ancho de pies:", fontWeight = FontWeight.Bold)
                BulletPoint("Cortos: Hombros. Largos: Más que hombros. Driver: Máxima estabilidad.")
                Text("\nAlineación:", fontWeight = FontWeight.Bold)
                BulletPoint("Tren: Pies, caderas y hombros paralelos a la línea de tiro.")
                BulletPoint("Inclinación: Desde la cadera, columna recta. No te encorves.")
                BulletPoint("Brazos: Colgando sueltos y relajados verticalmente.")
            }
        }

        item { ManualHeader("3. Posición de la Pelota") }
        item {
            ManualCard {
                BulletPoint("Hierros cortos: Al centro del stance.")
                BulletPoint("Hierros medios: 2cm hacia el pie delantero.")
                BulletPoint("Maderas/Driver: Al talón del pie delantero (ataque ascendente).")
            }
        }

        item { ManualHeader("4-5. Backswing y Cima") }
        item {
            ManualCard {
                BulletPoint("En bloque: Los primeros 50cm mové hombros y brazos juntos.")
                BulletPoint("Sin muñecas: No las quiebres en el arranque.")
                BulletPoint("Giro: Hombros 90°, caderas 45°. Resistí con la pierna trasera.")
                BulletPoint("Muñeca plana: En la cima, la muñeca delantera debe estar plana.")
            }
        }

        item { ManualHeader("6-8. Bajada e Impacto") }
        item {
            ManualCard {
                BulletPoint("Secuencia: Inicia desde el piso (Pies -> Caderas -> Torso).")
                BulletPoint("Lag: Manos bajan por delante de la cabeza del palo.")
                BulletPoint("Impacto: Cabeza estable. Manos por delante de la bola para comprimir.")
                BulletPoint("Finish: Pecho al objetivo, equilibrio sobre pierna delantera.")
            }
        }
    }
}

@Composable
fun ShortGameSection() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { ManualHeader("Chipping: El Péndulo") }
        item {
            ManualCard {
                Text("Tiro de bajo riesgo para volar poco y rodar mucho.")
                Text("\nSetup:", fontWeight = FontWeight.Bold)
                BulletPoint("Pies juntos: Mitad del ancho normal.")
                BulletPoint("Peso adelante: 70% en la pierna delantera siempre.")
                BulletPoint("Manos adelantadas: Varilla inclinada hacia el hoyo.")
                Text("\nEjecución:", fontWeight = FontWeight.Bold)
                BulletPoint("Triángulo: Mové hombros y brazos como una pieza rígida.")
                BulletPoint("Cero muñecas: Quebrarlas causa filazos o golpes pesados.")
            }
        }

        item { ManualHeader("Putting: Control Total") }
        item {
            ManualCard {
                Text("Alineación (Truco Vertical):", fontWeight = FontWeight.Bold)
                BulletPoint("Ojos directamente sobre la bola.")
                BulletPoint("Prueba: Soltá una bola desde tu nariz; debe golpear la del suelo.")
                Text("\nLectura de Green:", fontWeight = FontWeight.Bold)
                BulletPoint("Evaluá desde lejos la pendiente general al caminar al green.")
                BulletPoint("Leé desde atrás (1.5m) para ver el arco y punto de quiebre.")
                BulletPoint("Velocidad: Manda sobre la dirección. Un putt fuerte borra caída.")
            }
        }

        item { ManualHeader("Bunkers: El Colchón") }
        item {
            ManualCard {
                Text("El palo nunca toca la bola, solo la arena.")
                BulletPoint("Cara abierta: Girá el palo hacia afuera antes de tomar el grip.")
                BulletPoint("Punto de impacto: Entrá 2-4cm antes de la pelota.")
                BulletPoint("Swing decidido: No frenes el palo en la arena.")
                BulletPoint("Visualización: Imaginá la bola sobre un billete; debés remover todo el billete.")
            }
        }
    }
}

@Composable
fun DistanceSection() {
    val distances = listOf(
        listOf("Driver", "160–195 m", "Tee shot. Técnica."),
        listOf("Madera 3", "145–170 m", "Alternativa segura."),
        listOf("Híbrido 4", "135–155 m", "Fácil de elevar."),
        listOf("Híbrido 5", "125–145 m", "Comodín total."),
        listOf("Hierro 6", "115–130 m", "Hierro largo límite."),
        listOf("Hierro 7", "105–120 m", "Palo de práctica."),
        listOf("Hierro 8", "95–110 m", "Mucho control."),
        listOf("Hierro 9", "85–100 m", "Aproximación."),
        listOf("PW", "75–90 m", "Chips y cortos."),
        listOf("SW", "50–65 m", "Arena y corto."),
        listOf("LW", "35–50 m", "Mucha altura.")
    )

    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
        item { ManualHeader("Distancias Principiante") }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                        Text("Palo", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Text("Metros", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Text("Uso", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.5f))
                    }
                    HorizontalDivider()
                    distances.forEach { row ->
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                            Text(row[0], style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            Text(row[1], style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                            Text(row[2], style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1.5f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StrategySection() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                "GUÍA DEFINITIVA DE GESTIÓN DE CAMPO",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary
            )
        }

        item { ManualHeader("1. Estrategia en el Tee de Salida") }
        item {
            ManualCard {
                Text("Usá todo el ancho del Tee Box:", fontWeight = FontWeight.Bold)
                BulletPoint("Si el peligro está a la derecha: Parate a la derecha y tirá en diagonal hacia la izquierda.")
                BulletPoint("Si el peligro está a la izquierda: Parate a la izquierda y tirá hacia la derecha.")
                
                Text("\nObjetivo Hiperespecífico:", fontWeight = FontWeight.Bold)
                BulletPoint("No apuntes 'al medio'. Elegí un objetivo pequeño (un árbol lejano, una copa). 'Aim small, miss small'.")
                
                Text("\nDriver vs Madera 3:", fontWeight = FontWeight.Bold)
                BulletPoint("Driver: Fairways anchos o hoyos largos (Par 5).")
                BulletPoint("Madera 3: Fairways estrechos, peligros a tu distancia de Driver, doglegs o viento fuerte en contra.")
            }
        }

        item { ManualHeader("2. Selecciones Tácticas") }
        item {
            ManualCard {
                Text("Hierros Largos (H3-H5):", fontWeight = FontWeight.Bold)
                BulletPoint("Usalos desde fairway limpio y seco para trayectorias bajas y penetrantes.")
                
                Text("\nHíbridos (H3-H5):", fontWeight = FontWeight.Bold)
                BulletPoint("Ideales desde el rough liviano (suela ancha resbala).")
                BulletPoint("Perfectos para aterrizajes suaves en green desde lejos.")
                BulletPoint("Sustituto ideal de hierros largos: Sweet spot mucho más grande.")
            }
        }

        item { ManualHeader("3. La Regla de Oro en los Wedges") }
        item {
            ManualCard {
                Text("Control sobre Potencia", fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.error)
                BulletPoint("Jamás al 100%: Genera demasiado spin, 'globos' y gran dispersión lateral.")
                BulletPoint("Regla del 80%: Jugalos a un 75-80% de esfuerzo, buscando ritmo y contacto centrado.")
                BulletPoint("Control de distancia: Graduá acortando la subida (3/4 o 1/2 swing) en lugar de pegarle más suave.")
            }
        }

        item { ManualHeader("4. Estrategia desde el Rough") }
        item {
            ManualCard {
                Text("Evaluá el 'Lie' (apoyo):", fontWeight = FontWeight.Bold)
                BulletPoint("Hundida: Olvidate del green. Sacala al fairway con un palo de mucho loft (SW/PW).")
                BulletPoint("Apoyada arriba: Jugala normal.")
                
                Text("\nAjuste Técnico:", fontWeight = FontWeight.Bold)
                BulletPoint("Subí de palo: El pasto quita 20-40% de velocidad. Usá un palo más largo.")
                BulletPoint("Abrí la cara: El pasto tiende a cerrar la cara al atrapar el cuello de la varilla.")
                BulletPoint("Ataque inclinado: Pelota un poco atrás y bajada más empinada.")
            }
        }

        item { ManualHeader("5. Cómo Jugar con Viento") }
        item {
            ManualCard {
                Text("Viento en Contra:", fontWeight = FontWeight.Bold)
                BulletPoint("Error común: Pegarle más fuerte (genera más spin y la bola 'se cuelga').")
                BulletPoint("Estrategia: Usá 2 palos más largos y hacé un swing suave al 70%. Vuelo bajo y penetrante.")
                
                Text("\nViento a Favor:", fontWeight = FontWeight.Bold)
                BulletPoint("Bajá 1 palo. Tené en cuenta que la bola rodará mucho más al aterrizar.")
                
                Text("\nViento Cruzado:", fontWeight = FontWeight.Bold)
                BulletPoint("Apuntá al margen desde donde viene el viento y dejá que la brisa la traiga al centro.")
            }
        }

        item { ManualHeader("6. Gestión de Riesgos") }
        item {
            ManualCard {
                BulletPoint("Apuntá al Centro: Si la bandera está cerca de agua o búnker, buscá el centro del green. Dos putts aseguran el score.")
                BulletPoint("Saber hacer un Layup: En Par 5 o ante agua, asegurá un tiro de wedge cómodo (ej. 80m) en vez de arriesgar una madera imposible.")
                BulletPoint("Aceptá el 'Tiro Malo': Si estás en el bosque, jugá de costado al fairway. Perdé 1 golpe, evitá un desastre de 4.")
                BulletPoint("Mantené la Rutina: Mismos 20 segundos antes de CADA golpe.")
            }
        }
    }
}

@Composable
fun EtiquetteSection() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { ManualHeader("Seguridad y Ritmo") }
        item {
            ManualCard {
                Text("¡FORE!:", fontWeight = FontWeight.Bold, color = Color.Red)
                Text("Gritalo de inmediato si tu bola va hacia alguien. Si lo escuchás, cubrite la cabeza.")
                BulletPoint("Ready Golf: Pegá cuando estés listo y sea seguro.")
                BulletPoint("Provisional: Tirala si sospechás que la primera se perdió.")
                BulletPoint("3 Minutos: Tiempo máximo de búsqueda.")
                BulletPoint("Preparación: Mientras otros pegan, medí y elegí tu palo.")
            }
        }

        item { ManualHeader("Cuidado del Campo") }
        item {
            ManualCard {
                BulletPoint("Divots: Volvé a poner el trozo de césped y pisalo firmemente.")
                BulletPoint("Piques: Usá el arreglapiques moviendo bordes al centro sin romper raíz.")
                BulletPoint("Búnker: Rastrillá todas las huellas y dejá el rastrillo afuera paralelo.")
            }
        }

        item { ManualHeader("Respeto y Tecnología") }
        item {
            ManualCard {
                Text("Uso del Teléfono:", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                Text("Está muy mal visto usar el teléfono para hablar durante la vuelta. Mantenelo en silencio y usalo solo para emergencias o tu app de golf.")
                
                Spacer(Modifier.height(8.dp))
                Text("Concentración:", fontWeight = FontWeight.Bold)
                BulletPoint("Silencio absoluto durante el swing de un compañero.")
                BulletPoint("Cuidá tu sombra: Que no se proyecte sobre la bola o línea de putt ajena.")
            }
        }
    }
}

@Composable
fun RoutineSection() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                "RITUAL PRE-GOLPE (PRE-SHOT ROUTINE)",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary
            )
        }

        item {
            ManualCard {
                Text("Es la herramienta más poderosa para ganar consistencia. Su objetivo es cambiar el chip del cerebro: de la mente analítica (técnica) a la mente atlética (fluidez).")
                Text("\nUna buena rutina dura entre 15 y 20 segundos y se divide en dos zonas físicas:", style = MaterialTheme.typography.bodySmall)
            }
        }

        item { ManualHeader("1. La Zona de Decisión (Detrás)") }
        item {
            ManualCard {
                Text("A 1 metro detrás de la bola. Es el único lugar para pensar y dudar.", fontWeight = FontWeight.Bold)
                BulletPoint("Paso 1: Evaluar. Mirá distancia, viento, peligros y lie.")
                BulletPoint("Paso 2: Decidir. Elegí el palo exacto y el tipo de tiro.")
                BulletPoint("Paso 3: Visualizar. Imaginá la pelota volando con la altura y curva deseada.")
                BulletPoint("Paso 4: Spot Alignment. Buscá una referencia (yuyo, hojita) a 1m de tu bola directo al objetivo. Es más fácil apuntar a 1 metro que a 150.")
            }
        }

        item { ManualHeader("2. El Swing de Práctica") }
        item {
            ManualCard {
                Text("Transición rítmica:", fontWeight = FontWeight.Bold)
                BulletPoint("Hacé 1 o 2 swings suaves por detrás de la pelota.")
                BulletPoint("Clave: No es para corregir técnica, sino para sentir el peso del palo y la velocidad del tiro visualizado.")
            }
        }

        item { ManualHeader("3. La Zona de Ejecución (Al lado)") }
        item {
            ManualCard {
                Text("La mente analítica se apaga por completo.", fontWeight = FontWeight.Bold)
                BulletPoint("Paso 1: Apuntar. Colocá la cara del palo mirando al punto intermedio (a 1m).")
                BulletPoint("Paso 2: Setup. Acomodá el cuerpo paralelo a esa línea.")
                BulletPoint("Paso 3: Relax. Respirá profundo, soltá hombros y aflojá presión de manos.")
                BulletPoint("Paso 4: Último vistazo. Fotografiá el objetivo, volvé a la bola y... ¡pegale!")
                
                Spacer(Modifier.height(8.dp))
                Text("⚠️ La Regla de los 3 Segundos:", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                Text("No te congeles. Más de 5 segundos estático mirando la bola genera dudas y tensión.")
            }
        }

        item { ManualHeader("🚨 Las 2 Reglas de Oro") }
        item {
            ManualCard {
                Text("1. Si algo interrumpe, ABORTÁ:", fontWeight = FontWeight.Bold)
                Text("Ruido, viento o incomodidad = Salite de la bola. Volvé atrás y empezá de cero. El 90% de los tiros incómodos salen mal.")
                
                Spacer(Modifier.height(8.dp))
                Text("2. Replicá el mismo ritmo siempre:", fontWeight = FontWeight.Bold)
                Text("La rutina debe ser idéntica para un putt de 1m o un Driver. Esa repetición calma al sistema nervioso bajo presión.")
            }
        }
    }
}

@Composable
fun DoctorSection() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { ManualHeader("1. La pelota no se eleva (topada)") }
        item {
            ManualCard {
                Text("Causa: Levantar el cuerpo antes del impacto o peso atrás.", fontWeight = FontWeight.Bold)
                Text("\nCómo corregirlo:", fontWeight = FontWeight.Bold)
                BulletPoint("Peso adelante (60-70%) en la pierna delantera.")
                BulletPoint("Mantené la altura de la columna e inclinación.")
                BulletPoint("Pega 'hacia abajo' y sacá el divot delante de la bola.")
            }
        }

        item { ManualHeader("2. Curva a la derecha (Slice)") }
        item {
            ManualCard {
                Text("Causa: Trayectoria de afuera hacia adentro con cara abierta.", fontWeight = FontWeight.Bold)
                Text("\nCómo corregirlo:", fontWeight = FontWeight.Bold)
                BulletPoint("Revisá tu Grip: Girá manos a la derecha (ver 2-3 nudillos).")
                BulletPoint("Bajada: Inicia con caderas para bajar el palo 'por dentro'.")
                BulletPoint("Hombros: Mantenelos paralelos a los pies en el setup.")
            }
        }

        item { ManualHeader("3. Directo a la izquierda (Hook)") }
        item {
            ManualCard {
                Text("Causa: Cara cerrada o quiebre de muñecas muy rápido.", fontWeight = FontWeight.Bold)
                Text("\nCómo corregirlo:", fontWeight = FontWeight.Bold)
                BulletPoint("Grip neutro: Girá manos un poco a la izquierda (menos presión).")
                BulletPoint("Giro del torso: No frenes la rotación del pecho al bajar.")
                BulletPoint("Relajación: Evitá tensionar los antebrazos.")
            }
        }

        item { ManualHeader("4. Golpe pesado (Fat shot)") }
        item {
            ManualCard {
                Text("Causa: Pegar al suelo antes. Peso atrapado en pie trasero.", fontWeight = FontWeight.Bold)
                Text("\nCómo corregirlo:", fontWeight = FontWeight.Bold)
                BulletPoint("Transición fluida: La cadera debe trasladar el peso al inicio.")
                BulletPoint("Posición: Verificá que la bola no esté muy adelantada.")
                BulletPoint("Foco: Mirá la parte frontal de la pelota.")
            }
        }

        item { ManualHeader("5. Falta de distancia") }
        item {
            ManualCard {
                Text("Causa: Desaceleración o impacto fuera del sweet spot.", fontWeight = FontWeight.Bold)
                Text("\nCómo corregirlo:", fontWeight = FontWeight.Bold)
                BulletPoint("Aceleración progresiva: Máxima velocidad en el impacto.")
                BulletPoint("Ancho del swing: Brazo delantero extendido (gran radio).")
                BulletPoint("Finish: Pecho al objetivo garantiza seguimiento completo.")
            }
        }

        item { ManualHeader("6. El Shank (Socket / La Perrita)") }
        item {
            ManualCard {
                Text("Causa: Pegarle con el cuello (hosel) del palo. Estar muy cerca.", fontWeight = FontWeight.Bold)
                Text("\nCómo corregirlo:", fontWeight = FontWeight.Bold)
                BulletPoint("Espacio: Brazos colgando sueltos con margen a la bola.")
                BulletPoint("Balance: Mantené el peso en el medio del pie, no en las puntas.")
                BulletPoint("Camino del palo: Evitá empujar las manos hacia adelante al bajar.")
            }
        }
    }
}

@Composable
fun GlossarySection() {
    val glossary = listOf(
        "A" to listOf(
            "Address" to "Es la postura que adopta el jugador antes de iniciar el swing, apoyando el palo en el suelo.",
            "Albatros" to "Hacer un hoyo con 3 golpes bajo el par (ej. meterla en 2 en un Par 5).",
            "Approach" to "Tiro corto realizado para dejar la pelota sobre el green."
        ),
        "B" to listOf(
            "Backspin" to "Efecto de retroceso que frena la bola al tocar el green.",
            "Backswing" to "Fase donde se eleva el palo hasta la posición de la cima.",
            "Birdie" to "Completar un hoyo con 1 golpe bajo el par.",
            "Bogey" to "Completar un hoyo con 1 golpe sobre el par.",
            "Bunker" to "Depresión rellenada con arena que actúa como obstáculo."
        ),
        "C" to listOf(
            "Caddie" to "Persona que transporta la bolsa y asiste al jugador con consejos.",
            "Carry" to "Distancia que recorre la pelota por el aire antes de tocar el suelo.",
            "Chipping" to "Tiro corto de bajo vuelo y mucho rodamiento cerca del green.",
        ),
        "D" to listOf(
            "Divot" to "Trozo de césped que se levanta al golpear con un hierro o híbrido.",
            "Dogleg" to "Hoyo cuyo fairway forma una curva hacia la izquierda o derecha.",
            "Downswing" to "Fase de bajada del palo desde la cima hasta el impacto.",
            "Draw" to "Tiro controlado que curva suavemente de derecha a izquierda.",
            "Driver" to "Tiro de salida más largo (Madera 1).",
            "Dropar" to "Soltar una pelota desde la rodilla para ponerla en juego."
        ),
        "F" to listOf(
            "Fade" to "Tiro controlado que curva suavemente de izquierda a derecha.",
            "Fairway" to "Área de césped corto entre el tee y el green.",
            "Fore" to "Grito obligatorio de advertencia si una pelota sale desviada."
        ),
        "G" to listOf(
            "Gapping" to "Distribución escalonada de las distancias de cada palo.",
            "Green" to "Superficie de césped cortado al ras que rodea el hoyo.",
            "Green Fee" to "Tarifa para poder jugar en una cancha de golf.",
            "Grip" to "Empuñadura del palo y forma de colocar las manos.",
            "Gross" to "Suma total de golpes reales sin descontar hándicap."
        ),
        "H" to listOf(
            "Hándicap" to "Índice que mide la capacidad teórica de un golfista.",
            "Hazard" to "Área de penalización (lagos, arroyos, etc).",
            "Hook" to "Golpe defectuoso que curva excesivamente de derecha a izquierda."
        ),
        "M" to listOf(
            "Match Play" to "Modalidad donde se compite hoyo por hoyo.",
            "Medal Play" to "Modalidad donde se suma el total de golpes de 18 hoyos."
        ),
        "O" to listOf(
            "Out of Bounds" to "Zona exterior a los límites del campo (estacas blancas)."
        ),
        "P" to listOf(
            "Par" to "Número estándar de golpes para completar un hoyo.",
            "Pitch Mark" to "Marca que deja la bola en el green. Debe repararse.",
            "Pitching Wedge" to "Hierro especializado para aproximaciones (44°-48°).",
            "Pull" to "Tiro que sale recto pero desviado a la izquierda.",
            "Push" to "Tiro que sale recto pero desviado a la derecha.",
            "Putt" to "Golpe ejecutado sobre el green para rodar la bola al hoyo."
        ),
        "R" to listOf(
            "Rough" to "Césped más alto y tupido que flanquea el fairway."
        ),
        "S" to listOf(
            "Sand Wedge" to "Palo especializado para búnkers y tiros altos cortos.",
            "Scratch" to "Jugador con hándicap 0.",
            "Setup" to "Momento previo al swing (postura, alineación, grip).",
            "Slice" to "Tiro que curva incontrolablemente hacia la derecha.",
            "Stableford" to "Sistema de puntos basado en el score neto vs par.",
            "Stroke" to "Término formal para cada golpe ejecutado.",
            "Sweet Spot" to "Punto exacto del centro de la cara del palo."
        ),
        "T" to listOf(
            "Tee" to "Clavija para elevar la bola en el primer golpe de cada hoyo.",
            "Top" to "Error donde se golpea la mitad superior de la bola."
        )
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        glossary.forEach { (letter, terms) ->
            item {
                Text(
                    text = letter,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            items(terms) { (term, definition) ->
                ManualCard {
                    Text(term, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Text(definition, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
fun RegulationSection() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { ManualHeader("👕 Código de Vestimenta") }
        item {
            ManualCard {
                Text("Los clubes mantienen normas estrictas. El incumplimiento puede denegar el acceso.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                Text("\nCaballeros:", fontWeight = FontWeight.Bold)
                BulletPoint("Chomba/Camisa con cuello (metida por dentro).")
                BulletPoint("Pantalón de vestir, gabardina o bermuda (largo rodilla).")
                BulletPoint("Calzado: Clavos plásticos/blandos. Metálicos prohibidos.")
                Text("\nDamas:", fontWeight = FontWeight.Bold)
                BulletPoint("Chombas con cuello o diseño formal.")
                BulletPoint("Pantalón, calzas de golf o faldas adecuadas.")
                Text("\nProhibido:", color = Color.Red, fontWeight = FontWeight.Bold)
                Text("Camisetas de fútbol, musculosas, remeras de algodón, jeans, pantalones cargo o deportivos.")
            }
        }

        item { ManualHeader("🗺️ Áreas de la Cancha") }
        item {
            ManualCard {
                BulletPoint("Tee de Salida: Se inicia el hoyo. Podés retrasar hasta 2 palos, nunca adelantar.")
                BulletPoint("Fairway: Césped corto, ideal para el juego.")
                BulletPoint("Rough: Césped alto lateral que penaliza la precisión.")
                BulletPoint("Penalty Areas: Estacas rojas/amarillas (agua o intransitables).")
                BulletPoint("GUR (Terreno en reparación): Estacas azules. Alivio obligatorio gratuito.")
                BulletPoint("Green: Superficie milimétrica para el putter.")
            }
        }

        item { ManualHeader("🎨 Colores de Salida") }
        item {
            ManualCard {
                BulletPoint("Negras (Championship): Profesionales y máximo nivel.")
                BulletPoint("Blancas/Azules: Caballeros bajo/medio hándicap.")
                BulletPoint("Amarillas: Caballeros senior o iniciales.")
                BulletPoint("Rojas: Damas, principiantes y niños.")
            }
        }

        item { ManualHeader("📉 Sistemas de Puntaje") }
        item {
            ManualCard {
                BulletPoint("Medal Play (Stroke Play): Suma de todos los golpes reales.")
                BulletPoint("Stableford: Puntos según el par y tu hándicap.")
                BulletPoint("Match Play: Competencia hoyo por hoyo.")
                BulletPoint("Equipos (Scramble/Fourball): Estrategia compartida en parejas.")
            }
        }

        item { ManualHeader("⏱️ Protocolo y Dinámica") }
        item {
            ManualCard {
                BulletPoint("Puntualidad: Presentarse 15 min antes del Tee Time.")
                BulletPoint("El Honor: En el hoyo 1 por sorteo. Luego, el mejor score del hoyo previo.")
                BulletPoint("Ritmo: Ready Golf (pegar si estás listo y es seguro).")
                BulletPoint("Tiempo Tómite: Máximo 40 segundos por tiro.")
            }
        }

        item { ManualHeader("📜 Reglas Fundamentales") }
        item {
            ManualCard {
                BulletPoint("Límite de Palos: Máximo 14 palos en la bolsa.")
                BulletPoint("OB (Fuera de Límites): Estacas blancas. 1 golpe penal y distancia.")
                BulletPoint("Perdida: Máximo 3 minutos de búsqueda.")
                BulletPoint("Provisional: Tirala si sospechás pérdida o OB.")
                BulletPoint("Penalty Areas: 1 golpe penal. Amarillas (línea atrás), Rojas (suman 2 palos lateral).")
                BulletPoint("Drop: Desde la altura de la rodilla, erguido.")
            }
        }

        item { ManualHeader("⛳ Normas en el Green") }
        item {
            ManualCard {
                BulletPoint("Marcado: Colocar marca/moneda detrás de la bola antes de levantar.")
                BulletPoint("Piques: Reparar marcas de caída con arreglapiques.")
                BulletPoint("Bandera: Podés dejarla puesta al puttear. Sin penalidad si la golpeás.")
                BulletPoint("Movida accidental: Reponer sin penalidad (viento o postura).")
            }
        }

        item { ManualHeader("🤝 Etiqueta Social") }
        item {
            ManualCard {
                BulletPoint("¡FORE!: Gritar fuerte ante cualquier desvío con riesgo.")
                BulletPoint("Silencio: Absoluto durante la rutina de un compañero.")
                BulletPoint("Sombras: No proyectar sobre la línea de putt ajena.")
                BulletPoint("Búnker: Rastrillar todas las huellas al salir.")
            }
        }
    }
}

@Composable
fun ManualHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.ExtraBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
fun ManualCard(content: @Composable ColumnScope.() -> Unit) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            content()
        }
    }
}

@Composable
fun BulletPoint(text: String) {
    Row(modifier = Modifier.padding(vertical = 2.dp)) {
        Text("•", fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 8.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}
