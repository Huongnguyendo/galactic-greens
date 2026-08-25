package com.doquynhhuong.project.network

import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Retrofit interface for TheMealDB API.
 * Base URL: https://www.themealdb.com/api/json/v1/1/
 */
interface MealApiService {
    @GET("categories.php")
    suspend fun getCategories(): CategoryListResponse

    @GET("filter.php")
    suspend fun getMealsByCategory(@Query("c") category: String): MealListResponse

    @GET("lookup.php")
    suspend fun getMealDetail(@Query("i") mealId: String): MealDetailResponse

    @GET("search.php")
    suspend fun searchMeals(@Query("s") query: String): MealListResponse
}
