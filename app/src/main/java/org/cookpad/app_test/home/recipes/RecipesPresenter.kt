package org.cookpad.app_test.home.recipes

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject
import org.cookpad.app_test.Pipelines
import org.cookpad.app_test.RecipeAction
import org.cookpad.app_test.RecipeActionBookmark
import org.cookpad.app_test.RecipeActionLike
import org.cookpad.app_test.data.RecipeRepository
import org.cookpad.app_test.data.models.Recipe
import org.cookpad.app_test.utils.extensions.addTo

class RecipesPresenter(private val view: View,
                       private val repository: RecipeRepository = RecipeRepository()) : LifecycleObserver {
    private val disposables = CompositeDisposable()

    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    fun onCreate() {
        view.apply {
            detailClicks
                    .subscribe { recipe -> goToRecipeScreen(recipe.id) }
                    .addTo(disposables)

            fun updateRecipe(recipeAction: RecipeAction) = repository.updateRecipe(recipeAction.recipe).doOnComplete {
                // Notify the BookmarksFragment of the new bookmarked/unbookmarked recipe
                onRecipeUpdatedSubject?.onNext(recipeAction.recipe)
                Pipelines.recipeActionPipeline.channel(recipeAction.recipe.id).emit(recipeAction)
                showRecipes()
            }

            // We should assume that the views have already update themselves from the adapter
            likeClicks
                    .flatMapCompletable { recipe -> updateRecipe(RecipeActionLike(recipe.copy(liked = !recipe.liked))) }
                    .subscribe()
                    .addTo(disposables)

            bookmarkClicks
                    .flatMapCompletable { recipe -> updateRecipe(RecipeActionBookmark(recipe.copy(bookmarked = !recipe.bookmarked))) }
                    .subscribe()
                    .addTo(disposables)

            onRecipeUpdated?.subscribe { showRecipes() }
                    ?.addTo(disposables)

            onRecipeActionPipeline.subscribe { showRecipes(); println("recipes") }
                    .addTo(disposables)
        }

        showRecipes()
    }

    private fun showRecipes() {
        view.apply {
            repository.getAll()
                    .subscribe { recipes -> showRecipes(recipes) }
                    .addTo(disposables)
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun onDestroy() {
        disposables.dispose()
    }

    interface View {
        val detailClicks: PublishSubject<Recipe>
        val likeClicks: PublishSubject<Recipe>
        val bookmarkClicks: PublishSubject<Recipe>
        val onRecipeUpdatedSubject: PublishSubject<Recipe>?
        val onRecipeUpdated: Observable<Recipe>?

        val onRecipeActionPipeline: Observable<RecipeAction>

        fun showRecipes(recipes: List<Recipe>)
        fun goToRecipeScreen(recipeId: String)
    }
}