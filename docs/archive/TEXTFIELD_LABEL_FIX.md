# TextField Label Text Fix for Material3 Tests

## Problem
Tests were failing with:
```
java.lang.AssertionError: Failed to assert the following: (Text + EditableText = [])
Semantics of the node:
```

Affected tests:
- `textInputScreen_displaysInputField`
- `textInputScreen_clearButton_clearsInput`

## Root Cause
Material3's `OutlinedTextField` includes the `label` parameter in its **Text** semantics. When using `assertTextEquals(..., includeEditableText = true)`, the assertion checks:
1. **Text semantics** (which includes the label)
2. **EditableText semantics** (which contains the actual input value)

So for a TextField defined as:
```kotlin
OutlinedTextField(
    value = inputText,
    label = { Text("Enter text to translate") },
    // ...
)
```

The semantics tree looks like:
- Text: "Enter text to translate"
- EditableText: "" (empty when no input)

## Solution
Use `assertTextEquals(...)` to supply the exact sequence of `Text + EditableText` exposed by Material3. Do not set the `includeEditableText` flag (defaults are correct):
```kotlin
// ✅ Empty & unfocused: label + empty EditableText
composeTestRule
    .onNodeWithTag("input_text_field")
    .assertTextEquals("Enter text to translate", "")

// ✅ Empty & focused (placeholder visible): label + placeholder + empty EditableText
composeTestRule
    .onNodeWithTag("input_text_field")
    .assertTextEquals("Enter text to translate", "Type your message here...", "")
```

## The Pattern
When testing an empty OutlinedTextField with a label:
```kotlin
// Unfocused: label + empty EditableText
assertTextEquals("Label text here", "")

// Focused (placeholder visible): label + placeholder + empty EditableText
assertTextEquals("Label text here", "Placeholder here...", "")
```

When testing a filled OutlinedTextField:
```kotlin
// Check that the input text is present (ignores label)
assertTextContains("User input text", substring = true)
```

## Why This Works Better
The approach:
- ✅ Matches Material3's actual `Text + EditableText` semantics ordering
- ✅ Avoids the complexity of the `includeEditableText` parameter
- ✅ Clearly accounts for placeholder visibility when focused
- ✅ Uses `assertTextContains` for checking filled content

## Files Changed
- `app/src/androidTest/java/com/example/globaltranslation/ui/textinput/TextInputScreenTest.kt`
  - `textInputScreen_displaysInputField()` → asserts `(label, "")`
  - `textInputScreen_clearButton_clearsInput()` → asserts `(label, placeholder, "")` when focused
- `COMPOSE_BOM_2025_10_00_FIX.md` - Updated with final pattern
- `TESTING_IMPROVEMENTS_SUMMARY.md` - Documented best practice

## Key Takeaway
For Material3's OutlinedTextField:
- **Empty unfocused**: `assertTextEquals("label", "")`
- **Empty focused** (placeholder visible): `assertTextEquals("label", "placeholder", "")`
- **Filled field**: `assertTextContains("content")`
- **Optional**: Use `hasTextExactly(..., includeEditableText = false)` to ignore the editable part
