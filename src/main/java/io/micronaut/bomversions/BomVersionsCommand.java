package io.micronaut.bomversions;

import io.micronaut.configuration.picocli.PicocliRunner;
import jakarta.inject.Inject;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

@Command(name = "bom-versions", description = "...",
        mixinStandardHelpOptions = true)
public class BomVersionsCommand implements Runnable {

    @Option(names = {"-v", "--verbose"}, description = "...")
    boolean verbose;

    @Inject
    GithubApiClient githubApiClient;

    @Inject
    GithubConfiguration configuration;

    public static void main(String[] args) throws Exception {
        PicocliRunner.run(BomVersionsCommand.class, args);
    }

    public void run() {
        run(new File("/Users/sdelamo/github/micronaut-projects/micronaut-core/gradle/libs.versions.toml"));
    }
    public void run(File f) {

        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = br.readLine()) != null) {
                String prefix = "managed-micronaut";
                if (line.startsWith(prefix) && line.contains(" = ") && !line.contains("module")) {
                    String projectName = line.substring("managed-".length(), line.indexOf(" = \""));
                    String version = line.substring(line.indexOf(" = \"") + " = \"".length()).replaceAll("\"", "");
                    Project project = new Project(projectName, version);
                    GithubRelease githubRelease = githubApiClient.latest(configuration.getOrganization(), project.getName());
                    if (githubRelease == null) {
                        System.out.println("Could not fetch release for project: " + project.getName());
                    } else {
                        String githubReleaseVersion = githubRelease.getTagName().replace("v", "");
                        if (!githubReleaseVersion.equals(project.getVersion())) {
                            System.out.println(project.getName() + " bom version: " + project.getVersion() + " github version: " + githubReleaseVersion);
                        }
                    }

                }
                // process the line.
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // business logic here
        if (verbose) {
            System.out.println("Hi!");
        }
    }
}
