package org.springframework.cloud.stream.app.plugin.utils;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.maven.model.Build;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.DeploymentRepository;
import org.apache.maven.model.Developer;
import org.apache.maven.model.DistributionManagement;
import org.apache.maven.model.Exclusion;
import org.apache.maven.model.License;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.model.Profile;
import org.apache.maven.model.Scm;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import org.springframework.cloud.stream.app.plugin.Bom;
import org.springframework.util.StringUtils;

/**
 * @author Soby Chacko
 * @author Gunnar Hillert
 */
public class MavenModelUtils {

    private MavenModelUtils() {

    }

    public static Model populateModel(String artifactId, String groupId, String version) {
        Model model = new Model();
        model.setGroupId(groupId);
        model.setArtifactId(artifactId);
        model.setPackaging("pom");
        model.setVersion(version);
        model.setModelVersion("4.0.0");

        getBuildWithDockerPluginDefinition(model);

        return model;
    }

    private static void getBuildWithDockerPluginDefinition(Model model) {
        Build build = new Build();
        model.setBuild(build);
        Plugin plugin = new Plugin();
        plugin.setGroupId("io.fabric8");
        plugin.setArtifactId("docker-maven-plugin");
        plugin.setVersion("0.14.2");
        build.addPlugin(plugin);
    }

    public static Model getModelFromContainerPom(File genProjecthome, String groupId, String version) throws IOException, XmlPullParserException {
        File pom = new File(genProjecthome, "pom.xml");
        Model model = pom.exists() ? getModel(pom) : null;
        if (model != null) {
            model.setGroupId(groupId);
            model.setArtifactId(genProjecthome.getName());
            model.setVersion(version);

            model.setName("Apps Container");
            model.setDescription("Container project for generated apps");
            model.setUrl("https://spring.io/spring-cloud");
            License license = new License();
            license.setName("Apache License, Version 2.0");
            license.setUrl("https://www.apache.org/licenses/LICENSE-2.0");
            license.setComments("Copyright 2014-2017 the original author or authors.\n" +
                    "\n" +
                    "Licensed under the Apache License, Version 2.0 (the \"License\");\n" +
                    "you may not use this file except in compliance with the License.\n" +
                    "You may obtain a copy of the License at\n" +
                    "\n" +
                    "\thttp://www.apache.org/licenses/LICENSE-2.0\n" +
                    "\n" +
                    "Unless required by applicable law or agreed to in writing, software\n" +
                    "distributed under the License is distributed on an \"AS IS\" BASIS,\n" +
                    "WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or\n" +
                    "implied.\n" +
                    "\n" +
                    "See the License for the specific language governing permissions and\n" +
                    "limitations under the License.");
            List<License> licenses = new ArrayList<>();
            licenses.add(license);
            model.setLicenses(licenses);
            Scm scm = new Scm();
            scm.setConnection("scm:git:git://github.com/spring-cloud/spring-cloud-stream-app-starters.git");
            scm.setDeveloperConnection("scm:git:ssh://git@github.com/spring-cloud/spring-cloud-stream-app-starters.git");
            scm.setUrl("https://github.com/spring-cloud/spring-cloud-stream-app-starters");
            model.setScm(scm);

            Developer developer = new Developer();
            developer.setId("schacko");
            developer.setName("Soby Chacko");
            developer.setEmail("schacko at pivotal.io");
            developer.setOrganization("Pivotal Software, Inc.");
            developer.setOrganizationUrl("https://www.spring.io");
            List<String> roles = new ArrayList<>();
            roles.add("developer");
            developer.setRoles(roles);
            List<Developer> developers = new ArrayList<>();
            developers.add(developer);
            model.setDevelopers(developers);

            DistributionManagement distributionManagement = new DistributionManagement();

            DeploymentRepository releaseRepo = new DeploymentRepository();
            releaseRepo.setId("repo.spring.io");
            releaseRepo.setName("Spring Release Repository");
            releaseRepo.setUrl("https://repo.spring.io/libs-release-local");
            distributionManagement.setRepository(releaseRepo);

            DeploymentRepository snapshotRepo = new DeploymentRepository();
            snapshotRepo.setId("repo.spring.io");
            snapshotRepo.setName("Spring Snapshot Repository");
            snapshotRepo.setUrl("https://repo.spring.io/libs-snapshot-local");
            distributionManagement.setSnapshotRepository(snapshotRepo);

            model.setDistributionManagement(distributionManagement);

            Profile profile = new Profile();
            profile.setId("milestone");
            DistributionManagement milestoneDistManagement = new DistributionManagement();

            DeploymentRepository milestoneRepo = new DeploymentRepository();
            milestoneRepo.setId("repo.spring.io");
            milestoneRepo.setName("Spring Milestone Repository");
            milestoneRepo.setUrl("https://repo.spring.io/libs-milestone-local");
            milestoneDistManagement.setRepository(milestoneRepo);
            profile.setDistributionManagement(milestoneDistManagement);
            List<Profile> profiles = new ArrayList<>();
            profiles.add(profile);
            model.setProfiles(profiles);

            getBuildWithDockerPluginDefinition(model);
        }
        return model;
    }

    public static boolean addModuleIntoModel(Model model, String module) {
        if (!model.getModules().contains(module)) {
            model.addModule(module);
        }
        return true;
    }

    public static void writeModelToFile(Model model, OutputStream os) throws IOException {
        final MavenXpp3Writer writer = new MavenXpp3Writer();
        OutputStreamWriter w = new OutputStreamWriter(os, "utf-8");
        writer.write(w, model);
        w.close();
    }

    private static Model getModel(File pom) throws IOException, XmlPullParserException {
        MavenXpp3Reader reader = new MavenXpp3Reader();
        return reader.read(new FileReader(pom));
    }

    public static Model getModel(InputStream is) {
        final MavenXpp3Reader reader = new MavenXpp3Reader();
        try {
            return reader.read(is);
        }
        catch (IOException | XmlPullParserException e) {
            throw new IllegalStateException(e);
        }
    }

    public static void addDockerPlugin(String artifactId, String version, String dockerHubOrg, InputStream is, OutputStream os) throws IOException {
        final MavenXpp3Reader reader = new MavenXpp3Reader();

        Model pomModel;
        try {
            pomModel = reader.read(is);
        }
        catch (IOException | XmlPullParserException e) {
            throw new IllegalStateException(e);
        }

        final Plugin dockerPlugin = new Plugin();
        dockerPlugin.setGroupId("io.fabric8");
        dockerPlugin.setArtifactId("docker-maven-plugin");
        dockerPlugin.setVersion("0.14.2");

        final Xpp3Dom mavenPluginConfiguration = new Xpp3Dom("configuration");

        final Xpp3Dom images = addElement(mavenPluginConfiguration, "images");

        final Xpp3Dom image = addElement(images, "image");
        if (!version.endsWith("BUILD-SNAPSHOT")) {
            addElement(image, "name", dockerHubOrg + "/${project.artifactId}:" + version);
        }
        else {
            addElement(image, "name", dockerHubOrg + "/${project.artifactId}");
        }

        final Xpp3Dom build = addElement(image, "build");
        addElement(build, "from", "anapsix/alpine-java:8");

        final Xpp3Dom volumes = addElement(build, "volumes");
        addElement(volumes, "volume", "/tmp");

        final Xpp3Dom entryPoint = new Xpp3Dom("entryPoint");
        build.addChild(entryPoint);

        final Xpp3Dom exec = new Xpp3Dom("exec");
        entryPoint.addChild(exec);

        addElement(exec, "arg", "java");
        addElement(exec, "arg", "-jar");
        addElement(exec, "arg", "/maven/" + artifactId + ".jar");

        final Xpp3Dom assembly = addElement(build, "assembly");
        addElement(assembly, "descriptor", "assembly.xml");

        dockerPlugin.setConfiguration(mavenPluginConfiguration);

        pomModel.getBuild().addPlugin(dockerPlugin);
        pomModel.toString();
        writeModelToFile(pomModel, os);
    }

    public static void addExtraPlugins(Model pomModel) throws IOException {

        pomModel.getBuild().addPlugin(getSurefirePlugin());
        pomModel.getBuild().addPlugin(getJavadocPlugin());
        pomModel.getBuild().addPlugin(getSourcePlugin());

        pomModel.getProperties().setProperty("skipTests", "true");
    }

    public static void addBomsWithHigherPrecedence(Model pomModel, String bomsWithHigherPrecedence) throws IOException {
        DependencyManagement dependencyManagement = pomModel.getDependencyManagement();
        int i = 0;
        String[] boms = StringUtils.commaDelimitedListToStringArray(bomsWithHigherPrecedence);
        for (String bom : boms) {
            String[] coordinates = StringUtils.delimitedListToStringArray(bom, ":");
            if (coordinates.length != 3) {
                throw new IllegalStateException("Coordinates for additional boms are not defined properly.\n" +
                        "It needs to follow a comma separated pattern of groupId:artifactId:version");
            }
            String groupId = coordinates[0];
            String artifactId = coordinates[1];
            String version = coordinates[2];

            Dependency dependency = new Dependency();
            dependency.setGroupId(groupId);
            dependency.setArtifactId(artifactId);
            dependency.setVersion(version);
            dependency.setType("pom");
            dependency.setScope("import");
            dependencyManagement.getDependencies().add(i++, dependency);
        }

        pomModel.setDependencyManagement(dependencyManagement);
    }

    public static void addExclusionsForKafka(Model pomModel) throws IOException {

        List<Dependency> dependencies = pomModel.getDependencies();
        CopyOnWriteArrayList<Dependency> cowal = new CopyOnWriteArrayList<>(dependencies);
        for (Dependency dep : cowal) {
            if (dep.getArtifactId().startsWith("kafka_")) {
                pomModel.removeDependency(dep);
                Exclusion exclusion = new Exclusion();
                exclusion.setArtifactId("slf4j-log4j12");
                exclusion.setGroupId("org.slf4j");
                dep.addExclusion(exclusion);
                pomModel.addDependency(dep);
            }
        }
    }

    public static void addAdditionalBoms(Model pomModel, List<Bom> additionalBoms) throws IOException {
        DependencyManagement dependencyManagement = pomModel.getDependencyManagement();
        int i = 0;
        for (Bom bom : additionalBoms) {
            Dependency dependency = new Dependency();
            dependency.setGroupId(bom.getGroupId());
            dependency.setArtifactId(bom.getArtifactId());
            dependency.setVersion(bom.getVersion());
            dependency.setType("pom");
            dependency.setScope("import");
            dependencyManagement.getDependencies().add(++i, dependency);
        }

        pomModel.setDependencyManagement(dependencyManagement);
    }

    public static void addProperties(Model pomModel, Properties properties) {
        Properties pomProperties = pomModel.getProperties();
        if (properties != null) {
            for (Map.Entry<Object, Object> entry : properties.entrySet()) {
                pomProperties.put(entry.getKey(), entry.getValue());
            }
        }
    }

    public static void addDistributionManagement(Model pomModel) {
        DistributionManagement distributionManagement = new DistributionManagement();

        DeploymentRepository releaseRepo = new DeploymentRepository();
        releaseRepo.setId("repo.spring.io");
        releaseRepo.setName("Spring Release Repository");
        releaseRepo.setUrl("https://repo.spring.io/libs-release-local");
        distributionManagement.setRepository(releaseRepo);

        DeploymentRepository snapshotRepo = new DeploymentRepository();
        snapshotRepo.setId("repo.spring.io");
        snapshotRepo.setName("Spring Snapshot Repository");
        snapshotRepo.setUrl("https://repo.spring.io/libs-snapshot-local");
        distributionManagement.setSnapshotRepository(snapshotRepo);

        pomModel.setDistributionManagement(distributionManagement);
    }

    public static void addProfiles(Model pomModel) {
        Profile profile = new Profile();
        profile.setId("milestone");
        DistributionManagement milestoneDistManagement = new DistributionManagement();

        DeploymentRepository milestoneRepo = new DeploymentRepository();
        milestoneRepo.setId("repo.spring.io");
        milestoneRepo.setName("Spring Milestone Repository");
        milestoneRepo.setUrl("https://repo.spring.io/libs-milestone-local");
        milestoneDistManagement.setRepository(milestoneRepo);
        profile.setDistributionManagement(milestoneDistManagement);
        List<Profile> profiles = new ArrayList<>();
        profiles.add(profile);

        profiles.add(centralProfile());

        pomModel.setProfiles(profiles);
    }

    private static Profile centralProfile() {
        Profile centralProfile = new Profile();
        centralProfile.setId("central");
        DistributionManagement centralDistManagement = new DistributionManagement();

        DeploymentRepository snapshotRepo = new DeploymentRepository();
        snapshotRepo.setId("sonatype-nexus-snapshots");
        snapshotRepo.setName("Sonatype Nexus Snapshots");
        snapshotRepo.setUrl("https://oss.sonatype.org/content/repositories/snapshots/");
        centralDistManagement.setSnapshotRepository(snapshotRepo);

        DeploymentRepository repo = new DeploymentRepository();
        repo.setId("sonatype-nexus-staging");
        repo.setName("Nexus Release Repository");
        repo.setUrl("https://oss.sonatype.org/service/local/staging/deploy/maven2/");
        centralDistManagement.setRepository(repo);

        Plugin gpgPlugin = new Plugin();
        gpgPlugin.setGroupId("org.apache.maven.plugins");
        gpgPlugin.setArtifactId("maven-gpg-plugin");

        PluginExecution pluginExecution = new PluginExecution();
        pluginExecution.setId("sign-artifacts");
        pluginExecution.setPhase("verify");
        List<String> goals = new ArrayList<>();
        goals.add("sign");
        pluginExecution.setGoals(goals);
        gpgPlugin.getExecutions().add(pluginExecution);
        Build build = new Build();
        build.addPlugin(gpgPlugin);
        centralProfile.setBuild(build);

        centralProfile.setDistributionManagement(centralDistManagement);
        return centralProfile;
    }

    private static Plugin getSurefirePlugin() {
        final Plugin surefirePlugin = new Plugin();
        surefirePlugin.setGroupId("org.apache.maven.plugins");
        surefirePlugin.setArtifactId("maven-surefire-plugin");
        surefirePlugin.setVersion("2.19.1");
        final Xpp3Dom mavenPluginConfiguration = new Xpp3Dom("configuration");
        final Xpp3Dom skipTests = new Xpp3Dom("skipTests");
        skipTests.setValue("${skipTests}");
        mavenPluginConfiguration.addChild(skipTests);

        surefirePlugin.setConfiguration(mavenPluginConfiguration);
        return surefirePlugin;
    }

    private static Plugin getJavadocPlugin() {
        final Plugin javadocPlugin = new Plugin();
        javadocPlugin.setGroupId("org.apache.maven.plugins");
        javadocPlugin.setArtifactId("maven-javadoc-plugin");
        //javadocPlugin.setVersion("2.10.4");

        PluginExecution pluginExecution = new PluginExecution();
        pluginExecution.setId("javadoc");
        List<String> goals = new ArrayList<>();
        goals.add("jar");
        pluginExecution.setGoals(goals);
        pluginExecution.setPhase("package");
        List<PluginExecution> pluginExecutions = new ArrayList<>();
        pluginExecutions.add(pluginExecution);
        javadocPlugin.setExecutions(pluginExecutions);

        final Xpp3Dom javadocConfig = new Xpp3Dom("configuration");
        final Xpp3Dom quiet = new Xpp3Dom("quiet");
        quiet.setValue("true");
        javadocConfig.addChild(quiet);

        javadocPlugin.setConfiguration(javadocConfig);
        return javadocPlugin;
    }

    private static Plugin getSourcePlugin() {
        final Plugin sourcePlugin = new Plugin();
        sourcePlugin.setGroupId("org.apache.maven.plugins");
        sourcePlugin.setArtifactId("maven-source-plugin");

        PluginExecution pluginExecution = new PluginExecution();
        pluginExecution.setId("attach-sources");
        List<String> goals = new ArrayList<>();
        goals.add("jar");
        pluginExecution.setGoals(goals);
        pluginExecution.setPhase("package");
        List<PluginExecution> pluginExecutions = new ArrayList<>();
        pluginExecutions.add(pluginExecution);
        sourcePlugin.setExecutions(pluginExecutions);

        return sourcePlugin;
    }

    private static Xpp3Dom addElement(Xpp3Dom parentElement, String elementName) {
        return addElement(parentElement, elementName, null);
    }

    private static Xpp3Dom addElement(Xpp3Dom parentElement, String elementName, String elementValue) {
        Xpp3Dom child = new Xpp3Dom(elementName);
        child.setValue(elementValue);
        parentElement.addChild(child);
        return child;
    }

}

