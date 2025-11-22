package com.smartcloudsolutions.tollingvision.samples.patternbuilder;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/** Test class for FilenamePreview functionality. */
class FilenamePreviewTest {

  @Test
  void testConstructorWithAllParameters() {
    FilenamePreview preview =
        new FilenamePreview("test.jpg", "group1", ImageRole.FRONT, true, null);

    assertEquals("test.jpg", preview.getFilename());
    assertEquals("group1", preview.getGroupId());
    assertEquals(ImageRole.FRONT, preview.getRole());
    assertTrue(preview.isMatched());
    assertNull(preview.getErrorMessage());
    assertFalse(preview.hasError());
    assertTrue(preview.isSuccessful());
  }

  @Test
  void testConstructorWithFilenameOnly() {
    FilenamePreview preview = new FilenamePreview("test.jpg");

    assertEquals("test.jpg", preview.getFilename());
    assertNull(preview.getGroupId());
    assertNull(preview.getRole());
    assertFalse(preview.isMatched());
    assertNull(preview.getErrorMessage());
    assertFalse(preview.hasError());
    assertFalse(preview.isSuccessful());
  }

  @Test
  void testUpdateResults() {
    FilenamePreview preview = new FilenamePreview("test.jpg");

    preview.updateResults("group2", ImageRole.REAR, true, null);

    assertEquals("group2", preview.getGroupId());
    assertEquals(ImageRole.REAR, preview.getRole());
    assertTrue(preview.isMatched());
    assertNull(preview.getErrorMessage());
    assertTrue(preview.isSuccessful());
  }

  @Test
  void testSetError() {
    FilenamePreview preview = new FilenamePreview("test.jpg");

    preview.setError("Invalid pattern");

    assertNull(preview.getGroupId());
    assertNull(preview.getRole());
    assertFalse(preview.isMatched());
    assertEquals("Invalid pattern", preview.getErrorMessage());
    assertTrue(preview.hasError());
    assertFalse(preview.isSuccessful());
  }

  @Test
  void testSetSuccess() {
    FilenamePreview preview = new FilenamePreview("test.jpg");

    preview.setSuccess("group3", ImageRole.OVERVIEW);

    assertEquals("group3", preview.getGroupId());
    assertEquals(ImageRole.OVERVIEW, preview.getRole());
    assertTrue(preview.isMatched());
    assertNull(preview.getErrorMessage());
    assertTrue(preview.isSuccessful());
  }

  @Test
  void testHasError() {
    FilenamePreview preview = new FilenamePreview("test.jpg");

    assertFalse(preview.hasError());

    preview.setErrorMessage("Some error");
    assertTrue(preview.hasError());

    preview.setErrorMessage("");
    assertFalse(preview.hasError());

    preview.setErrorMessage("   ");
    assertFalse(preview.hasError());
  }

  @Test
  void testPropertyBindings() {
    FilenamePreview preview = new FilenamePreview("test.jpg");

    // Test that properties are properly bound
    assertNotNull(preview.filenameProperty());
    assertNotNull(preview.groupIdProperty());
    assertNotNull(preview.roleProperty());
    assertNotNull(preview.matchedProperty());
    assertNotNull(preview.errorMessageProperty());

    // Test property updates
    preview.setFilename("new.jpg");
    assertEquals("new.jpg", preview.filenameProperty().get());

    preview.setGroupId("newGroup");
    assertEquals("newGroup", preview.groupIdProperty().get());

    preview.setRole(ImageRole.FRONT);
    assertEquals(ImageRole.FRONT, preview.roleProperty().get());

    preview.setMatched(true);
    assertTrue(preview.matchedProperty().get());

    preview.setErrorMessage("error");
    assertEquals("error", preview.errorMessageProperty().get());
  }

  @Test
  void testToString() {
    FilenamePreview preview =
        new FilenamePreview("test.jpg", "group1", ImageRole.FRONT, true, null);

    String result = preview.toString();

    assertTrue(result.contains("test.jpg"));
    assertTrue(result.contains("group1"));
    assertTrue(result.contains("FRONT"));
    assertTrue(result.contains("true"));
  }
}
