package org.cookpad.rxbroadcaster_app_test.home.bookmarks

import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LifecycleObserver
import android.arch.lifecycle.OnLifecycleEvent
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject
import org.cookpad.rxbroadcaster_app_test.data.RecipeRepository
import org.cookpad.rxbroadcaster_app_test.data.models.Recipe
import org.cookpad.rxbroadcaster_app_test.utils.extensions.addTo

class BookmarksPresenter(private val view: View,
                         private val repository: RecipeRepository = RecipeRepository()) : LifecycleObserver {
    private val disposables = CompositeDisposable()

    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    fun onCreate() {
        view.apply {
            detailClicks
                    .subscribe { recipe -> goToRecipeScreen(recipe.id) }
                    .addTo(disposables)

            likeClicks
                    .flatMapCompletable { recipe ->
                        val updatedRecipe = recipe.copy(liked = !recipe.liked)
                        repository.updateRecipe(updatedRecipe).doOnComplete {
                            // Notify the RecipesFragment of the new bookmarked/unbookmarked recipe
                            onRecipeUpdatedSubject?.onNext(updatedRecipe)
                            showBookmarks()
                        }
                    }
                    .subscribe()
                    .addTo(disposables)

            bookmarkClicks
                    .flatMapCompletable { recipe ->
                        val updatedRecipe = recipe.copy(bookmarked = !recipe.bookmarked)
                        repository.updateRecipe(updatedRecipe).doOnComplete {
                            // Notify the RecipesFragment of the new bookmarked/unbookmarked recipe
                            onRecipeUpdatedSubject?.onNext(updatedRecipe)
                            showBookmarks()
                        }
                    }
                    .subscribe()
                    .addTo(disposables)

            onRecipeUpdated?.subscribe {
                // Update the bookmarks when notified from the RecipesFragment
                showBookmarks()
            }?.addTo(disposables)
        }

        showBookmarks()
    }

    private fun showBookmarks() {
        view.apply {
            repository.getBookmarks()
                    .subscribe { recipes -> showBookmarks(recipes) }
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
        var onRecipeUpdatedSubject: PublishSubject<Recipe>?
        var onRecipeUpdated: Observable<Recipe>?

        fun showBookmarks(recipes: List<Recipe>)
        fun goToRecipeScreen(recipeId: String)
    }
}
