# Fix for Compose BOM 2025.10.00 Material3 TextField Test Failures

## Latest Fix (Updated - Final)
The tests were failing with:
```
java.lang.AssertionError: Failed to assert the following: (Text + EditableText = [Enter text to translate,])
Semantics of the node:
```

**Root Cause**: With Compose BOM 2025.10.00, `assertTextEquals` compares the combined semantics sequence of `Text + EditableText`. For an OutlinedTextField with a label:
- Unfocused + empty → `Text = [label]`, `EditableText = ""` → expected list: `[label, ""]`
- Focused + empty (placeholder visible) → `Text = [label, placeholder]`, `EditableText = ""` → expected list: `[label, placeholder, ""]`

**Solution**: Supply the exact sequence to `assertTextEquals(...)` and avoid the `includeEditableText` flag entirely (defaults are correct):
- Empty (unfocused): `.assertTextEquals("Enter text to translate", "")`
- Empty (focused, placeholder visible): `.assertTextEquals("Enter text to translate", "Type your message here...", "")`

---

## Previous Issue Summary
The `TextInputScreenTest` was initially failing with errors like:
```
AssertionError: Failed: assertExists.
Reason: Expected exactly '1' node but could not find any node that satisfies: 
((Text + InputText + EditableText contains 'Hello' (ignoreCase: false)) && 
(hasAnyAncestorThat(TestTag = 'input_text_field')))
```

## Root Cause
Compose BOM 2025.10.00 introduced breaking changes to Material3's `OutlinedTextField` semantics:
- Placeholder text is no longer accessible via `hasText()` matcher
- Entered text is no longer searchable in the semantics tree (even with `useUnmergedTree = true`)
- The previous workaround documented in `TESTING_IMPROVEMENTS_SUMMARY.md` stopped working

## Solution
Changed test assertions to use direct TextField property access instead of node searches:

### Before (Broken):
```kotlin
// Failed to find text in semantics tree
composeTestRule
    .onNode(
        hasText("Hello") and hasAnyAncestor(hasTestTag("input_text_field")),
        useUnmergedTree = true
    )
    .assertExists()

// Failed to find placeholder
composeTestRule
    .onNode(
        hasText("Type your message here..."),
        useUnmergedTree = true
    )
    .assertExists()
```

### After (Fixed):
```kotlin
// Directly assert on TextField's text property for entered text
composeTestRule
    .onNodeWithTag("input_text_field")
    .assertTextContains("Hello", substring = true)

// Check empty state - unfocused: label + empty EditableText
composeTestRule
    .onNodeWithTag("input_text_field")
    .assertTextEquals("Enter text to translate", "")

// Check empty state - focused: label + placeholder + empty EditableText
composeTestRule
    .onNodeWithTag("input_text_field")
    .assertTextEquals("Enter text to translate", "Type your message here...", "")
```

## Tests Fixed
1. **`textInputScreen_displaysInputField`** - Now verifies empty field state with `assertTextEquals("label")`
2. **`textInputScreen_enterText_displaysInField`** - Now uses `assertTextContains()` to verify entered text
3. **`textInputScreen_clearButton_clearsInput`** - Now verifies cleared state with `assertTextEquals("label")`

## Key Learning
For Material3's OutlinedTextField with a label:
- Empty (unfocused): use `assertTextEquals("label", "")`
- Empty (focused, placeholder visible): use `assertTextEquals("label", "placeholder", "")`
- Filled: use `assertTextContains("text", substring = true)` to check content
- Alternative: `hasTextExactly(..., includeEditableText = false)` if you want to ignore the editable text part entirely

## Why This Works
The new approach uses `assertTextEquals()` and `assertTextContains()` which:
- Work with Material3's TextField semantics structure
- Are stable across Material3 versions
- Don't depend on complex parameter combinations
- Are the recommended way to test text fields in Compose

**Key Insight**: `assertTextEquals` validates the sequence of `Text` (label and possibly placeholder) followed by the `EditableText` content. Provide all visible pieces in order.

## Files Changed
- `app/src/androidTest/java/com/example/globaltranslation/ui/textinput/TextInputScreenTest.kt`
- `TESTING_IMPROVEMENTS_SUMMARY.md` (updated documentation)

## Testing
Run the tests with:
```bash
./gradlew :app:connectedAndroidTest
```

The failing tests should now pass:
- `textInputScreen_enterText_displaysInField`
- `textInputScreen_displaysInputField`

## Future Considerations
When testing Material3 TextFields:
1. ✅ Use `assertTextEquals("label")` for empty TextField with label
2. ✅ Use `assertTextContains("text")` for checking filled TextField content
3. ✅ Always use test tags to identify fields
4. ❌ Don't use `includeEditableText = true` - it complicates assertions
5. ❌ Don't search for text nodes in the semantics tree
6. ❌ Don't rely on `useUnmergedTree` for TextField text

This pattern is now documented in `TESTING_IMPROVEMENTS_SUMMARY.md` for future reference.
