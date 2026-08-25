package com.doquynhhuong.project.data

import com.doquynhhuong.project.network.MealApiService
import com.doquynhhuong.project.network.MealCategory
import com.doquynhhuong.project.network.MealDetail
import com.doquynhhuong.project.network.MealSummary
import com.doquynhhuong.project.network.RetrofitClient

/**
 * Single source of truth for all meal-related data.
 * Wraps every API call in Result<T> so ViewModels never handle raw exceptions.
 */
class MealRepository(
    private val api: MealApiService = RetrofitClient.mealApiService
) {
    companion object {
        /** Source categories that become safe after their individual ingredients are checked. */
        private val MEAT_FREE_CATEGORIES = setOf(
            "breakfast", "dessert", "pasta", "side", "starter", "vegan", "vegetarian"
        )

        private val MEAT_TERMS = listOf(
            "beef", "chicken", "pork", "bacon", "ham", "lamb", "mutton", "goat",
            "turkey", "duck", "goose", "veal", "venison", "rabbit", "sausage",
            "chorizo", "pepperoni", "salami", "prosciutto", "pancetta", "meatball",
            "minced meat", "ground meat", "gelatin", "gelatine", "lard", "suet",
            "anchovy", "anchovies", "fish", "salmon", "tuna", "cod", "haddock",
            "shrimp", "prawn", "crab", "lobster", "mussel", "clam", "oyster",
            "scallop", "squid", "octopus", "seafood", "bone broth"
        )

        fun isMeatFreeCategory(category: String): Boolean =
            category.trim().lowercase() in MEAT_FREE_CATEGORIES

        fun supportedMeatFreeCategories(): Set<String> = MEAT_FREE_CATEGORIES

        private fun MealDetail.hasNoMeatIngredients(): Boolean = ingredientList().all { (ingredient, _) ->
            val normalized = ingredient.lowercase().replace(Regex("[^a-z ]"), " ")
            MEAT_TERMS.none { term ->
                Regex("(^|\\s)${Regex.escape(term)}(\\s|$)").containsMatchIn(normalized)
            }
        }
    }

    suspend fun getCategories(): Result<List<MealCategory>> = runCatching {
        api.getCategories().categories.filter { isMeatFreeCategory(it.name) }
    }

    suspend fun getMealsByCategory(category: String): Result<List<MealSummary>> = runCatching {
        require(isMeatFreeCategory(category)) { "Only meat-free categories are available" }
        val candidates = api.getMealsByCategory(category).meals ?: emptyList()
        buildList {
            // Detail calls are intentionally sequential. The public API can drop
            // requests when a large category is checked concurrently, resulting
            // in an incomplete Firestore seed.
            candidates.forEach { summary ->
                val detail = runCatching {
                    api.getMealDetail(summary.id).meals?.firstOrNull()
                }.getOrNull()
                if (detail?.hasNoMeatIngredients() == true) add(summary)
            }
        }
    }

    suspend fun getMealDetail(mealId: String): Result<MealDetail?> = runCatching {
        api.getMealDetail(mealId).meals?.firstOrNull()
    }

    suspend fun searchMeals(query: String): Result<List<MealSummary>> = runCatching {
        api.searchMeals(query).meals ?: emptyList()
    }
}
