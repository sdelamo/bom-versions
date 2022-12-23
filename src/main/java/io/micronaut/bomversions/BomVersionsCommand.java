package io.micronaut.bomversions;

import io.micronaut.configuration.picocli.PicocliRunner;
import io.micronaut.core.util.StringUtils;
import io.micronaut.core.version.SemanticVersion;
import jakarta.inject.Inject;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;

@Command(name = "bom-versions", description = "...",
        mixinStandardHelpOptions = true)
public class BomVersionsCommand implements Runnable {

    @Option(names = {"-v", "--verbose"}, description = "...")
    boolean verbose;

    @Option(names = {"-p", "--patch"})
    boolean patch = true;

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

    private String cleanupName(Project project) {
        String name = project.getName();
        if (name.equals("micronaut-mongo")) {
            return "micronaut-mongodb";
        } else if (name.equals("micronaut-discovery")) {
            return "micronaut-discovery-client";
        } else if (name.equals("micronaut-oraclecloud")) {
            return "micronaut-oracle-cloud";
        } else if (name.equals("micronaut-xml")) {
            return "micronaut-jackson-xml";
        }
        return name;
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
                    String name = cleanupName(project);
                    if (verbose) {
                        System.out.println("fetching releases from project: " + name);
                    }
                    List<GithubRelease> githubReleases = githubApiClient.releases(configuration.getOrganization(), name, 10);
                    if (githubReleases == null) {
                        System.out.println("Could not fetch releases for project: " + name);
                    } else {
                        githubReleases.stream()
                                .filter(release -> !release.isDraft())
                                .map(GithubRelease::getTagName)
                                .filter(v -> !(v.contains("M") || v.contains("RC") || !v.contains(".") || v.contains("untagged")))
                                .map(tagName -> tagName.replace("v", ""))
                                .filter(semanticVersion -> {
                                    if (patch) {
                                        String projectVersion = project.getVersion();
                                        String mayorMinor = projectVersion.substring(0, projectVersion.lastIndexOf("."));
                                        return semanticVersion.startsWith(mayorMinor);
                                    }
                                    return true;
                                })
                                .map(SemanticVersion::new)
                                .sorted(Comparator.reverseOrder())
                                .map(SemanticVersion::getVersion)
                                .findFirst()
                                .ifPresent(githubReleaseVersion -> {
                                    if (!githubReleaseVersion.equals(project.getVersion())) {
                                        System.out.println(name + " bom version: " + project.getVersion() + " github version: " + githubReleaseVersion);
                                    }
                        });
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
