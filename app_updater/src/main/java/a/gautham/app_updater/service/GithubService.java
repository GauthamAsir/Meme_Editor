package a.gautham.app_updater.service;

import a.gautham.app_updater.models.GitRelease;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface GithubService {
    @GET("/repos/{username}/{repo}/releases/latest")
    Call<GitRelease> getReleases(@Path("username") String username, @Path("repo") String repo);

}
