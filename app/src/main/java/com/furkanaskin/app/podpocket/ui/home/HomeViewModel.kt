package com.furkanaskin.app.podpocket.ui.home

import android.app.Application
import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.furkanaskin.app.podpocket.Podpocket
import com.furkanaskin.app.podpocket.core.BaseViewModel
import com.furkanaskin.app.podpocket.core.Resource
import com.furkanaskin.app.podpocket.core.Status
import com.furkanaskin.app.podpocket.db.entities.EpisodeEntity
import com.furkanaskin.app.podpocket.service.response.BestPodcasts
import com.furkanaskin.app.podpocket.service.response.EpisodeRecommendations
import com.furkanaskin.app.podpocket.service.response.PodcastRecommendations
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import org.jetbrains.anko.doAsync

/**
 * Created by Furkan on 16.04.2019
 */

class HomeViewModel(app: Application) : BaseViewModel(app) {

    private val disposable = CompositeDisposable()
    val bestPodcastsLiveData = MutableLiveData<Resource<BestPodcasts>>()
    val recommendedPodcastsLiveData = MutableLiveData<Resource<PodcastRecommendations>>()
    val recommendedEpisodesLiveData = MutableLiveData<Resource<EpisodeRecommendations>>()
    var podcastEpisodeIds = MutableLiveData<ArrayList<String>>()


    init {
        (app as? Podpocket)?.component?.inject(this)
    }

    fun getBestPodcasts(region: String, explicitContent: Int) {
        disposable.add(baseApi.getBestPodcasts(region, explicitContent)
                .subscribeOn(Schedulers.io())
                .map { Resource.success(it) }
                .onErrorReturn { Resource.error(it) }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    when (it?.status) {
                        Status.SUCCESS -> bestPodcastsLiveData.postValue(it)
                        Status.LOADING -> Log.v("BestPodcasts", "Loading")
                        Status.ERROR -> Log.v("BestPodcasts", "${it.error?.printStackTrace()}")
                    }
                })
    }

    fun getPodcastRecommendations(podcastId: String, explicitContent: Int) {
        disposable.add(baseApi.getPodcastRecommendations(podcastId, explicitContent)
                .subscribeOn(Schedulers.io())
                .map { Resource.success(it) }
                .onErrorReturn { Resource.error(it) }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    when (it?.status) {
                        Status.SUCCESS -> recommendedPodcastsLiveData.postValue(it)
                        Status.LOADING -> Log.v("BestPodcasts", "Loading")
                        Status.ERROR -> Log.v("BestPodcasts", "${it.error?.printStackTrace()}")
                    }
                })
    }

    fun getEpisodeRecommendations(podcastId: String, explicitContent: Int) {
        disposable.add(baseApi.getEpisodeRecommendations(podcastId, explicitContent)
                .subscribeOn(Schedulers.io())
                .map { Resource.success(it) }
                .onErrorReturn { Resource.error(it) }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    when (it?.status) {
                        Status.SUCCESS -> recommendedEpisodesLiveData.postValue(it)
                        Status.LOADING -> Log.v("BestPodcasts", "Loading")
                        Status.ERROR -> Log.v("BestPodcasts", "${it.error?.printStackTrace()}")
                    }
                })
    }

    fun getEpisodes(id: String) {
        disposable.add(baseApi.getPodcastById(id)
                .subscribeOn(Schedulers.io())
                .map { Resource.success(it) }
                .onErrorReturn { Resource.error(it) }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    when (it?.status) {
                        Status.SUCCESS -> {
                            val ids = ArrayList<String>()
                            it.data?.episodes?.forEachIndexed { _, episodesItem ->
                                ids.add(episodesItem?.id ?: "")
                            }

                            podcastEpisodeIds.postValue(ids)

                            doAsync {
                                db.episodesDao().deleteAllEpisodes()
                                it.data?.episodes?.forEachIndexed { _, episode ->
                                    val episodesItem = episode.let { EpisodeEntity(it!!) }
                                    episodesItem.let { db.episodesDao().insertEpisode(it) }
                                }
                            }
                        }
                        Status.LOADING -> Log.v("BestPodcasts", "Loading")
                        Status.ERROR -> Log.v("BestPodcasts", "${it.error?.printStackTrace()}")
                    }
                })
    }
}