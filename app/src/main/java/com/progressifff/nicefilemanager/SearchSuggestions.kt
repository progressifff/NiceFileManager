package com.progressifff.nicefilemanager

import android.arch.persistence.room.*
import io.reactivex.Single

@Entity
class Suggestion(@PrimaryKey @ColumnInfo(name = "name") var name: String){
    @ColumnInfo(name = "time")
    var time:  Long = System.currentTimeMillis()
}

@Dao
interface  SuggestionDao{

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(suggestion: Suggestion)

    @Query("SELECT name FROM suggestion ORDER BY time DESC")
    fun getAll(): Single<List<String>>

    @Query("DELETE FROM suggestion where time NOT IN (SELECT time from suggestion ORDER BY time DESC LIMIT 15)")
    fun truncateHistory()

    @Query("DELETE FROM suggestion")
    fun deleteAll()
}

@Database(entities = [Suggestion::class], version = 1, exportSchema = false)
abstract class SearchSuggestions : RoomDatabase() {
    abstract fun suggestionDao(): SuggestionDao

    companion object {
        val instance: SearchSuggestions by lazy {
            return@lazy Room.databaseBuilder(App.get(),
                    SearchSuggestions::class.java, "SearchSuggestionsDatabase").build()
        }
    }
}