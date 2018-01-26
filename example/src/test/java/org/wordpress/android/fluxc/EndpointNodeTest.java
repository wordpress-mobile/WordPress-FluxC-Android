package org.wordpress.android.fluxc;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.wordpress.android.fluxc.annotations.endpoint.EndpointNode;
import org.wordpress.android.fluxc.annotations.endpoint.EndpointTreeGenerator;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

@RunWith(RobolectricTestRunner.class)
public class EndpointNodeTest {
    @Test
    public void testEndpointNodeSetup() {
        EndpointNode root = new EndpointNode("/sites/");
        EndpointNode childNode = new EndpointNode("$site/");
        EndpointNode grandchildNode = new EndpointNode("posts/");

        childNode.addChild(grandchildNode);
        root.addChild(childNode);

        Assert.assertEquals("posts/", root.getChildren().get(0).getChildren().get(0).getLocalEndpoint());

        Assert.assertEquals("/sites/", grandchildNode.getRoot().getLocalEndpoint());
        Assert.assertEquals(root, grandchildNode.getParent().getParent());
        Assert.assertEquals("$site/", grandchildNode.getParent().getLocalEndpoint());
        Assert.assertEquals("/sites/$site/posts/", grandchildNode.getFullEndpoint());
    }

    @Test
    public void testGetCleanEndpointName() {
        EndpointNode node = new EndpointNode("$post_ID/");
        Assert.assertEquals("post", node.getCleanEndpointName());

        EndpointNode nodeWithType = new EndpointNode("$taxonomy#String/");
        Assert.assertEquals("taxonomy", nodeWithType.getCleanEndpointName());

        EndpointNode emptyNode = new EndpointNode("");
        Assert.assertEquals("", emptyNode.getCleanEndpointName());
    }

    @Test
    public void testGetEndpointTypes() {
        EndpointNode typedNode = new EndpointNode("$taxonomy#String/");
        Assert.assertEquals(1, typedNode.getEndpointTypes().size());
        Assert.assertEquals("String", typedNode.getEndpointTypes().get(0));

        EndpointNode normalNode = new EndpointNode("$post_ID/");
        Assert.assertTrue(normalNode.getEndpointTypes().isEmpty());
    }

    @Test
    public void testEndpointTreeGenerator() throws IOException {
        File temp = File.createTempFile("endpoints", ".txt");
        temp.deleteOnExit();

        // An empty file should return a root EndpointNode with no children
        EndpointNode endpointTree = EndpointTreeGenerator.process(temp);

        Assert.assertFalse(endpointTree.hasChildren());

        // An empty file (except for a newline) should return a root EndpointNode with no children
        BufferedWriter out = new BufferedWriter(new FileWriter(temp));
        out.newLine();
        out.close();

        endpointTree = EndpointTreeGenerator.process(temp);

        Assert.assertFalse(endpointTree.hasChildren());

        // A series of nested endpoints should be processed correctly as a single branch
        out = new BufferedWriter(new FileWriter(temp));
        out.write("/sites/");
        out.newLine();
        out.write("/sites/$site/");
        out.newLine();
        out.write("/sites/$site/posts");
        out.close();

        endpointTree = EndpointTreeGenerator.process(temp);

        Assert.assertEquals(1, endpointTree.getChildren().size());
        Assert.assertEquals("/sites/$site/posts/",
                endpointTree.getChildren().get(0).getChildren().get(0).getChildren().get(0).getFullEndpoint());

        // A duplicate endpoint entry should be ignored
        out = new BufferedWriter(new FileWriter(temp));
        out.write("/sites/");
        out.newLine();
        out.write("/sites/$site/");
        out.newLine();
        out.write("/sites/$site/posts");
        out.newLine();
        out.write("/sites/");
        out.close();

        endpointTree = EndpointTreeGenerator.process(temp);

        Assert.assertEquals(1, endpointTree.getChildren().size());
        Assert.assertEquals("/sites/$site/posts/",
                endpointTree.getChildren().get(0).getChildren().get(0).getChildren().get(0).getFullEndpoint());

        // A single nested endpoint should be processed as nested nodes
        out = new BufferedWriter(new FileWriter(temp));
        out.write("/sites/$site/posts");
        out.close();

        endpointTree = EndpointTreeGenerator.process(temp);

        Assert.assertEquals(1, endpointTree.getChildren().size());
        Assert.assertEquals("/sites/$site/posts/",
                endpointTree.getChildren().get(0).getChildren().get(0).getChildren().get(0).getFullEndpoint());

        // Two separate top-level endpoints should be processed correctly
        out = new BufferedWriter(new FileWriter(temp));
        out.write("/sites/");
        out.newLine();
        out.write("/me/");
        out.close();

        endpointTree = EndpointTreeGenerator.process(temp);

        Assert.assertEquals(2, endpointTree.getChildren().size());
        Assert.assertEquals("/", endpointTree.getLocalEndpoint());
    }
}
