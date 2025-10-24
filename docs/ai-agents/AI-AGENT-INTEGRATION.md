# AI Agent Integration Guide

This document explains how different instruction files work together to provide comprehensive guidance for AI coding assistants (GitHub Copilot, Cursor AI, etc.).

> **📁 Location Note**: All AI agent instructions have been consolidated in `docs/ai-agents/` directory.

## 📋 Instruction Files Overview

### 1. `.cursorrules` (Root Directory)
**Purpose**: Quick reference and Cursor-specific rules  
**Audience**: Cursor AI  
**Focus**: 
- Multi-module architecture structure
- Immediate best practices
- Critical build configuration
- 16KB page size support patterns
- Common patterns used in this project
- Quick troubleshooting reference

**When to consult**: 
- Starting a coding session
- Need quick pattern reference
- Troubleshooting build issues
- Verifying correct approach

### 2. `.github/copilot-instructions.md`
**Purpose**: Primary GitHub Copilot configuration  
**Audience**: GitHub Copilot (standard location)  
**Focus**:
- High-level project overview
- Multi-module architecture summary
- Critical build setup
- References to detailed documentation in `docs/`

**When to consult**:
- Initial project understanding
- Quick reference for build config
- Finding pointers to detailed docs

### 3. `docs/ai-agents/copilot-instructions.md`
**Purpose**: Comprehensive project documentation  
**Audience**: All AI assistants  
**Focus**:
- Complete architecture overview
- Detailed implementation patterns
- Full project context and status
- 16KB page size support implementation
- Testing strategies
- Future enhancement areas

**When to consult**:
- Understanding project architecture
- Implementing new features
- Learning project conventions
- Planning larger changes

### 4. `docs/ai-agents/copilot-analysis-rules.instructions.md`
**Purpose**: Error prevention and debugging  
**Audience**: All AI assistants  
**Focus**:
- Common mistake patterns
- Pre-change validation checklists
- Post-change verification steps
- Debugging workflows
- Context gathering rules
- 16KB page size compatibility checks

**When to consult**:
- Before making any code changes
- When encountering errors
- Debugging build issues
- Validating approach before implementation

### 4. `docs/planning/docs/planning/Project Plan.md` (Root Directory)
**Purpose**: Implementation status and history  
**Audience**: Developers, project managers, AI assistants  
**Focus**:
- Implementation completion status
- Build configuration details
- Recent fixes and their impacts
- Code quality verification results
- 16KB page size support completion
- Potential future enhancements

**When to consult**:
- Understanding what's implemented
- Checking current project status
- Reviewing recent fixes
- Planning future work

### 5. `README.md` (Root Directory)
**Purpose**: User-facing documentation  
**Audience**: Users, new developers, AI assistants  
**Focus**:
- Getting started guide
- Build requirements
- Project structure
- 16KB page size support information
- Troubleshooting common issues
- Production-ready status

**When to consult**:
- Setting up the project
- Understanding build requirements
- Troubleshooting setup issues
- Learning project features

## 🔄 How Files Work Together

### Example 1: Adding a New Feature

**Step 1**: Read `docs/planning/docs/planning/Project Plan.md`
- ✅ Verify feature isn't already implemented
- ✅ Check current architecture status
- ✅ Understand existing patterns
- ✅ Verify 16KB page size support status

**Step 2**: Read `docs/ai-agents/copilot-instructions.md`
- ✅ Learn detailed implementation patterns
- ✅ Review ViewModel and Service patterns
- ✅ Understand Hilt setup requirements
- ✅ Check 16KB page size implementation details
- ✅ Check testing approach

**Step 3**: Check `docs/ai-agents/copilot-analysis-rules.instructions.md`
- ✅ Review pre-change validation checklist
- ✅ Understand common mistake patterns
- ✅ Note architecture constraints
- ✅ Verify 16KB compatibility requirements

**Step 4**: Use `.cursorrules` during implementation
- ✅ Quick reference for StateFlow pattern
- ✅ Verify Hilt annotations
- ✅ Check code quality standards
- ✅ Ensure resource cleanup
- ✅ Verify 16KB page size patterns

**Step 5**: Validate with `docs/ai-agents/copilot-analysis-rules.instructions.md`
- ✅ Run post-change validation
- ✅ Verify no architecture violations
- ✅ Check build configuration unchanged
- ✅ Test 16KB build variant compatibility

### Example 2: Fixing a Build Error

**Step 1**: Check `README.md` Troubleshooting
- ✅ Look for matching error in troubleshooting section
- ✅ Try suggested quick fixes
- ✅ Check 16KB page size specific issues

**Step 2**: Consult `.cursorrules` Common Issues
- ✅ Review "Common Issues & Solutions"
- ✅ Check build configuration requirements
- ✅ Verify 16KB page size configuration

**Step 3**: Use `docs/ai-agents/copilot-analysis-rules.instructions.md`
- ✅ Follow debugging workflow
- ✅ Check version compatibility
- ✅ Verify plugin configuration
- ✅ Test 16KB build variant

**Step 4**: Verify against `docs/ai-agents/copilot-instructions.md`
- ✅ Confirm build system setup matches
- ✅ Check dependency versions
- ✅ Review critical build setup section
- ✅ Verify 16KB page size implementation

### Example 3: Code Review / Validation

**Step 1**: Use `.cursorrules` for Pattern Check
- ✅ Verify StateFlow pattern correct
- ✅ Check Hilt annotations present
- ✅ Confirm resource cleanup exists
- ✅ Verify 16KB page size patterns

**Step 2**: Validate with `docs/ai-agents/copilot-analysis-rules.instructions.md`
- ✅ Run through architecture validation rules
- ✅ Check for common mistake patterns
- ✅ Verify file-specific patterns
- ✅ Test 16KB build variant compatibility

**Step 3**: Compare against `docs/ai-agents/copilot-instructions.md`
- ✅ Ensure matches documented patterns
- ✅ Verify follows best practices
- ✅ Check integration points
- ✅ Verify 16KB page size implementation

**Step 4**: Update `docs/planning/docs/planning/Project Plan.md` if needed
- ✅ Document new implementation
- ✅ Note any issues found and fixed
- ✅ Update verification status
- ✅ Update 16KB page size support status

## 📊 Information Hierarchy

```
Quick Reference
    ↓
.cursorrules (Patterns & Quick Fixes)
    ↓
Detailed Implementation
    ↓
copilot-instructions.md (Architecture & Patterns)
    ↓
Error Prevention
    ↓
copilot-analysis-rules.instructions.md (Validation & Debugging)
    ↓
Status & History
    ↓
docs/planning/Project Plan.md (Implementation Status)
    ↓
User Documentation
    ↓
README.md (Setup & Troubleshooting)
```

## 🎯 Best Practices for AI Assistants

### Always Start With
1. Read `docs/planning/docs/planning/Project Plan.md` to understand current status
2. Check `.cursorrules` for quick pattern reference
3. Review relevant sections in `copilot-instructions.md`

### Before Making Changes
1. Read `docs/ai-agents/copilot-analysis-rules.instructions.md`
2. Run through pre-change validation checklist
3. Verify pattern in `.cursorrules`

### During Implementation
1. Reference `.cursorrules` for patterns
2. Follow examples in `copilot-instructions.md`
3. Maintain consistency with existing code

### After Making Changes
1. Validate against `copilot-analysis-rules.instructions.md`
2. Update `docs/planning/docs/planning/Project Plan.md` if significant change
3. Consider updating `README.md` if user-facing

## 🔍 Finding Information

### "What build configuration should I use?"
→ `.cursorrules` → Critical Build Rules section (includes 16KB support)
→ `README.md` → Build Requirements section
→ Verify: `gradle/libs.versions.toml`

### "How do I implement a ViewModel?"
→ `.cursorrules` → StateFlow Best Practices section
→ `copilot-instructions.md` → ViewModel Pattern section
→ Examples: Existing ViewModels in project

### "What's already implemented?"
→ `docs/planning/docs/planning/Project Plan.md` → Implementation Status sections
→ `README.md` → Completed Implementation section
→ Verify: Actual code in `app/src/main/java/`

### "Why is my build failing?"
→ `README.md` → Troubleshooting section
→ `.cursorrules` → Common Issues & Solutions
→ `copilot-analysis-rules.instructions.md` → Debugging Workflow

### "What patterns should I follow?"
→ `.cursorrules` → Code Quality Standards (includes 16KB patterns)
→ `copilot-instructions.md` → Key Implementation Patterns
→ Examples: Existing code in project

### "How do I avoid common mistakes?"
→ `copilot-analysis-rules.instructions.md` → Common Mistake Patterns
→ `.cursorrules` → DON'T section
→ Pre-change validation checklist

### "How do I ensure 16KB page size compatibility?"
→ `.cursorrules` → 16KB Page Size Support section
→ `copilot-instructions.md` → 16KB Page Size Support section
→ `copilot-analysis-rules.instructions.md` → 16KB compatibility checks
→ Test: `./gradlew :app:assembleSixteenKB`

## 📝 Maintenance Guidelines

### When to Update Each File

**`.cursorrules`**
- ✅ New critical build requirement discovered
- ✅ Common pattern established
- ✅ Frequent issue needs quick reference
- ✅ 16KB page size support patterns added

**`copilot-instructions.md`**
- ✅ Major architectural change
- ✅ New implementation pattern adopted
- ✅ Significant feature added
- ✅ 16KB page size support implemented
- ✅ Testing approach changes

**`copilot-analysis-rules.instructions.md`**
- ✅ New common mistake identified
- ✅ Debugging workflow improved
- ✅ Validation checklist updated
- ✅ 16KB page size compatibility checks added

**`docs/planning/docs/planning/Project Plan.md`**
- ✅ Feature implementation status changes
- ✅ Build configuration updated
- ✅ Significant bug fixed
- ✅ Code quality metrics updated
- ✅ 16KB page size support completion

**`README.md`**
- ✅ User-facing changes
- ✅ Setup process changes
- ✅ New troubleshooting solution
- ✅ Build requirements change
- ✅ 16KB page size support information added

## 🚀 Integration with AI Tools

### GitHub Copilot
- Reads all `.md` files in `docs/ai-agents/`
- Uses context from open files
- Applies `copilot-instructions.md` and `copilot-analysis-rules.instructions.md`

### Cursor AI
- Reads `.cursorrules` in root directory
- Also accesses `docs/ai-agents/` files
- Integrates with all markdown documentation
- Provides quick pattern matching from `.cursorrules`

### General AI Assistants
- Can access all markdown files when provided
- Use this integration guide to understand relationships
- Follow the hierarchy for information lookup

## 🎓 Learning Path for New AI Assistants

### First Session
1. Read `README.md` for project overview
2. Read `docs/planning/docs/planning/Project Plan.md` for current status
3. Scan `.cursorrules` for critical patterns
4. Bookmark `copilot-instructions.md` for details

### Before First Code Change
1. Read `docs/ai-agents/copilot-analysis-rules.instructions.md`
2. Understand pre-change validation
3. Review architecture constraints
4. Check common mistake patterns

### Ongoing Development
1. Keep `.cursorrules` open for quick reference
2. Consult `copilot-instructions.md` for complex patterns
3. Use `copilot-analysis-rules.instructions.md` for validation
4. Update `docs/planning/docs/planning/Project Plan.md` when implementing features

## ✅ Verification Checklist

When assisting with this codebase, verify:

- [ ] Read relevant instruction files before changes
- [ ] Followed patterns in `.cursorrules`
- [ ] Checked against `copilot-analysis-rules.instructions.md`
- [ ] Matches architecture in `copilot-instructions.md`
- [ ] Updated `docs/planning/docs/planning/Project Plan.md` if significant change
- [ ] Updated `README.md` if user-facing change
- [ ] Maintained consistency with existing code
- [ ] No architecture violations introduced
- [ ] Build configuration unchanged (unless intentional)
- [ ] StateFlow pattern maintained
- [ ] Hilt annotations correct
- [ ] Resource cleanup implemented

## 🔗 Cross-References

Throughout the instruction files, you'll find cross-references:

- **"See `docs/ai-agents/copilot-instructions.md`"**
  → For detailed implementation patterns
  
- **"See `.cursorrules`"**
  → For quick pattern reference
  
- **"See `docs/planning/docs/planning/Project Plan.md`"**
  → For implementation status
  
- **"See `README.md`"**
  → For user-facing documentation
  
- **"Follow `copilot-analysis-rules.instructions.md`"**
  → For validation and error prevention

These cross-references help navigate the documentation efficiently.

## 🎯 Summary

**For Quick Tasks**: Use `.cursorrules`  
**For Understanding**: Use `copilot-instructions.md`  
**For Validation**: Use `copilot-analysis-rules.instructions.md`  
**For Status**: Use `docs/planning/docs/planning/Project Plan.md`  
**For Users**: Use `README.md`  

**Together**, these files provide comprehensive guidance for maintaining code quality, following best practices, and avoiding common pitfalls in this production-ready Android translation app.

