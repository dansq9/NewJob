package app.ascend.games.engine.games.grouping

import app.ascend.games.engine.core.GameLanguage

/**
 * Original grouping puzzles, per content language. Expand these pools freely.
 * Category names are content (not UI chrome), so they live here rather than in
 * the app's string resources.
 */
object GroupingData {

    fun pool(language: GameLanguage): List<GroupingPuzzle> = when (language) {
        GameLanguage.PORTUGUESE -> PT
        GameLanguage.ENGLISH -> EN
    }

    private fun puzzle(vararg cats: GroupingCategory) = GroupingPuzzle(cats.toList())

    private val EN = listOf(
        puzzle(
            GroupingCategory("Types of interview", listOf("Phone", "Panel", "Group", "Mock"), 0),
            GroupingCategory("Resume sections", listOf("Skills", "Summary", "Education", "References"), 1),
            GroupingCategory("Work arrangements", listOf("Remote", "Hybrid", "Onsite", "Contract"), 2),
            GroupingCategory("Job benefits", listOf("Bonus", "Pension", "Insurance", "Leave"), 3),
        ),
        puzzle(
            GroupingCategory("Planets", listOf("Earth", "Mars", "Venus", "Saturn"), 0),
            GroupingCategory("___ ball", listOf("Base", "Basket", "Foot", "Volley"), 1),
            GroupingCategory("Citrus fruits", listOf("Lemon", "Lime", "Orange", "Pomelo"), 2),
            GroupingCategory("Chess pieces", listOf("King", "Queen", "Bishop", "Knight"), 3),
        ),
        puzzle(
            GroupingCategory("Colors", listOf("Amber", "Coral", "Olive", "Teal"), 0),
            GroupingCategory("Dog breeds", listOf("Boxer", "Husky", "Poodle", "Beagle"), 1),
            GroupingCategory("Card games", listOf("Poker", "Bridge", "Hearts", "Rummy"), 2),
            GroupingCategory("Bond actors", listOf("Connery", "Moore", "Craig", "Dalton"), 3),
        ),
        puzzle(
            GroupingCategory("Oceans", listOf("Pacific", "Atlantic", "Indian", "Arctic"), 0),
            GroupingCategory("Coffee drinks", listOf("Latte", "Mocha", "Espresso", "Americano"), 1),
            GroupingCategory("Keyboard keys", listOf("Shift", "Enter", "Tab", "Space"), 2),
            GroupingCategory("Greek letters", listOf("Alpha", "Beta", "Delta", "Sigma"), 3),
        ),
        puzzle(
            GroupingCategory("Seasons", listOf("Spring", "Summer", "Autumn", "Winter"), 0),
            GroupingCategory("Big cats", listOf("Lion", "Tiger", "Jaguar", "Leopard"), 1),
            GroupingCategory("Units of time", listOf("Second", "Minute", "Hour", "Decade"), 2),
            GroupingCategory("Monopoly tokens", listOf("Boot", "Thimble", "Hat", "Iron"), 3),
        ),
        puzzle(
            GroupingCategory("Body parts", listOf("Elbow", "Ankle", "Wrist", "Knee"), 0),
            GroupingCategory("Programming langs", listOf("Kotlin", "Swift", "Java", "Rust"), 1),
            GroupingCategory("Pizza toppings", listOf("Olive", "Onion", "Pepper", "Mushroom"), 2),
            GroupingCategory("___ light", listOf("Moon", "Spot", "Flash", "Sun"), 3),
        ),
        puzzle(
            GroupingCategory("Continents", listOf("Africa", "Europe", "Asia", "Australia"), 0),
            GroupingCategory("Musical notes", listOf("Whole", "Half", "Quarter", "Eighth"), 1),
            GroupingCategory("Board games", listOf("Chess", "Checkers", "Risk", "Clue"), 2),
            GroupingCategory("Shades of red", listOf("Crimson", "Scarlet", "Ruby", "Cherry"), 3),
        ),
    )

    private val PT = listOf(
        puzzle(
            GroupingCategory("Planetas", listOf("Terra", "Marte", "Vênus", "Saturno"), 0),
            GroupingCategory("Frutas", listOf("Manga", "Uva", "Limão", "Caju"), 1),
            GroupingCategory("Cores", listOf("Verde", "Azul", "Roxo", "Cinza"), 2),
            GroupingCategory("Peças de xadrez", listOf("Rei", "Rainha", "Bispo", "Cavalo"), 3),
        ),
        puzzle(
            GroupingCategory("Estações", listOf("Verão", "Outono", "Inverno", "Primavera"), 0),
            GroupingCategory("Animais", listOf("Gato", "Cavalo", "Coelho", "Macaco"), 1),
            GroupingCategory("Bebidas", listOf("Café", "Suco", "Leite", "Água"), 2),
            GroupingCategory("Instrumentos", listOf("Violão", "Flauta", "Piano", "Bateria"), 3),
        ),
        puzzle(
            GroupingCategory("Oceanos", listOf("Pacífico", "Atlântico", "Índico", "Ártico"), 0),
            GroupingCategory("Esportes", listOf("Futebol", "Vôlei", "Tênis", "Basquete"), 1),
            GroupingCategory("Talheres", listOf("Garfo", "Faca", "Colher", "Prato"), 2),
            GroupingCategory("Cômodos", listOf("Sala", "Quarto", "Cozinha", "Banheiro"), 3),
        ),
    )
}
