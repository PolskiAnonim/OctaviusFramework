package org.octavius.novels.domain.game

import org.octavius.novels.domain.NovelLanguage
import org.octavius.novels.domain.NovelStatus
import org.octavius.novels.form.ColumnInfo
import org.octavius.novels.form.ControlResultData
import org.octavius.novels.form.ForeignKey
import org.octavius.novels.form.Form
import org.octavius.novels.form.FormControls
import org.octavius.novels.form.SaveOperation
import org.octavius.novels.form.TableRelation
import org.octavius.novels.form.control.ComparisonType
import org.octavius.novels.form.control.ControlDependency
import org.octavius.novels.form.control.DependencyType
import org.octavius.novels.form.control.type.BooleanControl
import org.octavius.novels.form.control.type.DatabaseControl
import org.octavius.novels.form.control.type.EnumControl
import org.octavius.novels.form.control.type.IntegerControl
import org.octavius.novels.form.control.type.SectionControl
import org.octavius.novels.form.control.type.TextControl
import org.octavius.novels.form.control.type.TextListControl

class GameForm(id: Int? = null) : Form() {

    init {
        if (id != null) {
            // Ładowanie istniejącej gry
            loadData(id)
        } else {
            // Inicjalizacja formularza dla nowej gry
            clearForm()
        }
    }

    override fun defineTableRelations(): List<TableRelation> {
        return listOf(
            TableRelation("games"), // Główna tabela
            TableRelation("game_characters", "games.id = game_characters.game_id"), // Powiązana tabela
            TableRelation("game_play_time", "games.id = game_play_time.game_id"), // Powiązana tabela
            TableRelation("game_ratings", "games.id = game_ratings.game_id"), // Powiązana tabela
        )
    }

    override fun createSchema(): FormControls {
        return FormControls(
            mapOf(
                "gameSeries" to DatabaseControl(
                    ColumnInfo("games","series"),
                    label = "Seria",
                    relatedTable = "game_series",
                    displayColumn = "name",
                )
            ),
            listOf("gameSeries")
        )
    }

    override fun processFormData(formData: Map<String, Map<String, ControlResultData>>): List<SaveOperation> {
        return listOf()
    }
}