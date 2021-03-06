package org.apache.maven.plugin.install;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.Collections;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.metadata.ArtifactMetadata;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.install.stubs.AttachedArtifactStub0;
import org.apache.maven.plugin.install.stubs.InstallArtifactStub;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.shared.utils.io.FileUtils;
import org.sonatype.aether.impl.internal.EnhancedLocalRepositoryManager;
import org.sonatype.aether.util.DefaultRepositorySystemSession;

/**
 * @author <a href="mailto:aramirez@apache.org">Allan Ramirez</a>
 */

public class InstallMojoTest
    extends AbstractMojoTestCase
{

    InstallArtifactStub artifact;

    private final String LOCAL_REPO = "target/local-repo/";

    public void setUp()
        throws Exception
    {
        super.setUp();

        System.out.println( ">>>Cleaning local repo " + getBasedir() + "/" + LOCAL_REPO + "..." );

        FileUtils.deleteDirectory( new File( getBasedir() + "/" + LOCAL_REPO ) );
        
//        LegacySupport legacySupport = lookup( LegacySupport.class );
//        RepositorySystemSession repositorySession = new DefaultRepositorySystemSession();
//        MavenExecutionRequest executionRequest = new DefaultMavenExecutionRequest();
//        legacySupport.setSession( new MavenSession( getContainer(), repositorySession, executionRequest, null ) );
    }

    public void testInstallTestEnvironment()
        throws Exception
    {
        File testPom = new File( getBasedir(), "target/test-classes/unit/basic-install-test/plugin-config.xml" );

        AbstractInstallMojo mojo = (AbstractInstallMojo) lookupMojo( "install", testPom );

        assertNotNull( mojo );
    }

    public void testBasicInstall()
        throws Exception
    {
        File testPom = new File( getBasedir(), "target/test-classes/unit/basic-install-test/plugin-config.xml" );

        AbstractInstallMojo mojo = (AbstractInstallMojo) lookupMojo( "install", testPom );

        assertNotNull( mojo );

        File file = new File( getBasedir(), "target/test-classes/unit/basic-install-test/target/"
            + "maven-install-test-1.0-SNAPSHOT.jar" );

        MavenProject project = (MavenProject) getVariableValueFromObject( mojo, "project" );
        updateMavenProject( project );
        
        setVariableValueToObject( mojo, "reactorProjects", Collections.singletonList( project ) );
        setVariableValueToObject( mojo, "session", createMavenSession() );

        artifact = (InstallArtifactStub) project.getArtifact();

        artifact.setFile( file );

        mojo.execute();

        String groupId = dotToSlashReplacer( artifact.getGroupId() );

        File installedArtifact = new File( getBasedir(), LOCAL_REPO + groupId + "/" + artifact.getArtifactId() + "/" +
            artifact.getVersion() + "/" + artifact.getArtifactId() + "-" + artifact.getVersion() + "." + artifact.getArtifactHandler().getExtension() );

        assertTrue( installedArtifact.exists() );
        
        assertEquals( 5, FileUtils.getFiles( new File( LOCAL_REPO ), null, null ).size() );
    }

    public void testBasicInstallWithAttachedArtifacts()
        throws Exception
    {
        File testPom = new File( getBasedir(), "target/test-classes/unit/basic-install-test-with-attached-artifacts/"
            + "plugin-config.xml" );

        AbstractInstallMojo mojo = (AbstractInstallMojo) lookupMojo( "install", testPom );

        assertNotNull( mojo );

        MavenProject project = (MavenProject) getVariableValueFromObject( mojo, "project" );
        updateMavenProject( project );

        setVariableValueToObject( mojo, "reactorProjects", Collections.singletonList( project ) );
        setVariableValueToObject( mojo, "session", createMavenSession() );

        List<Artifact> attachedArtifacts = project.getAttachedArtifacts();

        mojo.execute();

        String packaging = project.getPackaging();

        String groupId;

        for ( Object attachedArtifact1 : attachedArtifacts )
        {
            AttachedArtifactStub0 attachedArtifact = (AttachedArtifactStub0) attachedArtifact1;

            groupId = dotToSlashReplacer( attachedArtifact.getGroupId() );

            File installedArtifact = new File( getBasedir(), LOCAL_REPO + groupId + "/" +
                attachedArtifact.getArtifactId() + "/" + attachedArtifact.getVersion() + "/" +
                attachedArtifact.getArtifactId() + "-" + attachedArtifact.getVersion() + "." + packaging );

            assertTrue( installedArtifact.getPath() + " does not exist", installedArtifact.exists() );
        }
        
        assertEquals( 13, FileUtils.getFiles( new File( LOCAL_REPO ), null, null ).size() );
    }

    public void testUpdateReleaseParamSetToTrue()
        throws Exception
    {
        File testPom = new File( getBasedir(), "target/test-classes/unit/configured-install-test/plugin-config.xml" );

        AbstractInstallMojo mojo = (AbstractInstallMojo) lookupMojo( "install", testPom );

        assertNotNull( mojo );

        File file = new File( getBasedir(), "target/test-classes/unit/configured-install-test/target/"
            + "maven-install-test-1.0-SNAPSHOT.jar" );

        MavenProject project = (MavenProject) getVariableValueFromObject( mojo, "project" );
        updateMavenProject( project );

        setVariableValueToObject( mojo, "reactorProjects", Collections.singletonList( project ) );
        setVariableValueToObject( mojo, "session", createMavenSession() );

        artifact = (InstallArtifactStub) project.getArtifact();

        artifact.setFile( file );

        mojo.execute();

        assertTrue( artifact.isRelease() );
        
        assertEquals( 5, FileUtils.getFiles( new File( LOCAL_REPO ), null, null ).size() );
    }

    public void testInstallIfArtifactFileIsNull()
        throws Exception
    {
        File testPom = new File( getBasedir(), "target/test-classes/unit/basic-install-test/plugin-config.xml" );

        AbstractInstallMojo mojo = (AbstractInstallMojo) lookupMojo( "install", testPom );

        assertNotNull( mojo );

        MavenProject project = (MavenProject) getVariableValueFromObject( mojo, "project" );
        updateMavenProject( project );

        setVariableValueToObject( mojo, "reactorProjects", Collections.singletonList( project ) );
        setVariableValueToObject( mojo, "session", createMavenSession() );

        artifact = (InstallArtifactStub) project.getArtifact();

        artifact.setFile( null );

        assertNull( artifact.getFile() );

        try
        {
            mojo.execute();

            fail( "Did not throw mojo execution exception" );
        }
        catch ( MojoExecutionException e )
        {
            //expected
        }
        
        assertFalse( new File( LOCAL_REPO ).exists() );
    }

    public void testInstallIfPackagingIsPom()
        throws Exception
    {
        File testPom = new File( getBasedir(),
                                 "target/test-classes/unit/basic-install-test-packaging-pom/" + "plugin-config.xml" );

        AbstractInstallMojo mojo = (AbstractInstallMojo) lookupMojo( "install", testPom );

        assertNotNull( mojo );

        MavenProject project = (MavenProject) getVariableValueFromObject( mojo, "project" );
        updateMavenProject( project );

        setVariableValueToObject( mojo, "reactorProjects", Collections.singletonList( project ) );
        setVariableValueToObject( mojo, "session", createMavenSession() );

        String packaging = project.getPackaging();

        assertEquals( "pom", packaging );

        artifact = (InstallArtifactStub) project.getArtifact();

        mojo.execute();

        String groupId = dotToSlashReplacer( artifact.getGroupId() );

        File installedArtifact = new File( getBasedir(), LOCAL_REPO + groupId + "/" + artifact.getArtifactId() + "/" +
            artifact.getVersion() + "/" + artifact.getArtifactId() + "-" + artifact.getVersion() + "." + "pom" );

        assertTrue( installedArtifact.exists() );
        
        assertEquals( 4, FileUtils.getFiles( new File( LOCAL_REPO ), null, null ).size() );
    }

    public void testBasicInstallAndCreateChecksumIsTrue()
        throws Exception
    {
        File testPom = new File( getBasedir(), "target/test-classes/unit/basic-install-checksum/plugin-config.xml" );

        AbstractInstallMojo mojo = (AbstractInstallMojo) lookupMojo( "install", testPom );

        assertNotNull( mojo );

        File file = new File( getBasedir(), "target/test-classes/unit/basic-install-checksum/" + "maven-test-jar.jar" );

        MavenProject project = (MavenProject) getVariableValueFromObject( mojo, "project" );
        updateMavenProject( project );

        setVariableValueToObject( mojo, "reactorProjects", Collections.singletonList( project ) );
        setVariableValueToObject( mojo, "session", createMavenSession() );

        artifact = (InstallArtifactStub) project.getArtifact();

        boolean createChecksum = (Boolean) getVariableValueFromObject( mojo, "createChecksum" );

        assertTrue( createChecksum );

        artifact.setFile( file );

        mojo.execute();

        ArtifactMetadata metadata = null;
        for ( Object o : artifact.getMetadataList() )
        {
            metadata = (ArtifactMetadata) o;
            if ( metadata.getRemoteFilename().endsWith( "pom" ) )
            {
                break;
            }
        }

        ArtifactRepository localRepo = (ArtifactRepository) getVariableValueFromObject( mojo, "localRepository" );

        File pom = new File( localRepo.getBasedir(), localRepo.pathOfLocalRepositoryMetadata( metadata, localRepo ) );

        assertTrue( pom.exists() );

        //get the actual checksum of the pom
        mojo.digester.calculate( pom );
        String actualPomMd5Sum = mojo.digester.getMd5();
        String actualPomSha1Sum = mojo.digester.getSha1();

        //get the actual checksum of the artifact
        mojo.digester.calculate( file );
        String actualMd5Sum = mojo.digester.getMd5();
        String actualSha1Sum = mojo.digester.getSha1();

        String groupId = dotToSlashReplacer( artifact.getGroupId() );

        String packaging = project.getPackaging();

        String localPath = getBasedir() + "/" + LOCAL_REPO + groupId + "/" + artifact.getArtifactId() + "/" +
            artifact.getVersion() + "/" + artifact.getArtifactId() + "-" + artifact.getVersion();

        File installedArtifact = new File( localPath + "." + packaging );

        File pomMd5 = new File( localPath + ".pom.md5" );
        File pomSha1 = new File( localPath + ".pom.sha1" );

        File md5 = new File( localPath + "." + packaging + ".md5" );
        File sha1 = new File( localPath + "." + packaging + ".sha1" );

        assertTrue( pomMd5.exists() );
        assertTrue( pomSha1.exists() );
        assertTrue( md5.exists() );
        assertTrue( sha1.exists() );

        String generatedMd5 = FileUtils.fileRead( md5, "UTF-8" );
        String generatedSha1 = FileUtils.fileRead( sha1, "UTF-8" );
        String generatedPomMd5 = FileUtils.fileRead( pomMd5, "UTF-8" );
        String generatedPomSha1 = FileUtils.fileRead( pomSha1, "UTF-8" );

        assertEquals( actualMd5Sum, generatedMd5 );
        assertEquals( actualSha1Sum, generatedSha1 );
        assertEquals( actualPomMd5Sum, generatedPomMd5 );
        assertEquals( actualPomSha1Sum, generatedPomSha1 );

        assertTrue( installedArtifact.exists() );
        
        assertEquals( 9, FileUtils.getFiles( new File( LOCAL_REPO ), null, null ).size() );
    }

    public void testSkip()
        throws Exception
    {
        File testPom = new File( getBasedir(), "target/test-classes/unit/basic-install-test/plugin-config.xml" );

        InstallMojo mojo = (InstallMojo) lookupMojo( "install", testPom );

        assertNotNull( mojo );

        File file = new File( getBasedir(), "target/test-classes/unit/basic-install-test/target/"
            + "maven-install-test-1.0-SNAPSHOT.jar" );

        MavenProject project = (MavenProject) getVariableValueFromObject( mojo, "project" );
        updateMavenProject( project );

        setVariableValueToObject( mojo, "reactorProjects", Collections.singletonList( project ) );
        setVariableValueToObject( mojo, "session", createMavenSession() );

        artifact = (InstallArtifactStub) project.getArtifact();

        artifact.setFile( file );

        mojo.setSkip( true );

        mojo.execute();

        String groupId = dotToSlashReplacer( artifact.getGroupId() );

        String packaging = project.getPackaging();

        File installedArtifact = new File( getBasedir(), LOCAL_REPO + groupId + "/" + artifact.getArtifactId() + "/" +
            artifact.getVersion() + "/" + artifact.getArtifactId() + "-" + artifact.getVersion() + "." + packaging );

        assertFalse( installedArtifact.exists() );
        
        assertFalse( new File( LOCAL_REPO ).exists() );
    }


    private String dotToSlashReplacer( String parameter )
    {
        return parameter.replace( '.', '/' );
    }
    
    private MavenSession createMavenSession()
    {
        MavenSession session = mock( MavenSession.class );
        DefaultRepositorySystemSession repositorySession  = new DefaultRepositorySystemSession();
        repositorySession.setLocalRepositoryManager( new EnhancedLocalRepositoryManager( new File( LOCAL_REPO )     ) );
        ProjectBuildingRequest buildingRequest = new DefaultProjectBuildingRequest();
        buildingRequest.setRepositorySession( repositorySession );
        when( session.getProjectBuildingRequest() ).thenReturn( buildingRequest );
        return session;
    }
    
    private void updateMavenProject( MavenProject project )
    {
       project.setGroupId( project.getArtifact().getGroupId() );
       project.setArtifactId( project.getArtifact().getArtifactId() );
       project.setVersion( project.getArtifact().getVersion() );
    }
}
