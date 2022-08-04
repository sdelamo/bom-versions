package io.micronaut.bomversions;

import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Header;
import io.micronaut.http.client.annotation.Client;
import static io.micronaut.http.HttpHeaders.ACCEPT;
import static io.micronaut.http.HttpHeaders.AUTHORIZATION;
import static io.micronaut.http.HttpHeaders.USER_AGENT;

@Client(GithubConfiguration.GITHUB_API_URL)
@Header(name = USER_AGENT, value = "Micronaut HTTP Client")
@Header(name = ACCEPT, value = "application/vnd.github.v3+json, application/json")
@Header(name = AUTHORIZATION, value = "token ${github.token}")
public interface GithubApiClient {

    @Get("/repos/{organization}/{repo}/releases/latest")
    GithubRelease latest(String organization, String repo);
}
