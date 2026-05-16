## MODIFIED Requirements

### Requirement: Dynamic Model Loading
The engine must support models that require external dictionary directories.

#### Scenario: Load Matcha model with dictionary
- **WHEN** the user selects the "zh-baker (Matcha)" model.
- **THEN** the application should copy the dictionary files from assets to external storage.
- **AND** initialize the engine with the correct `dictDir` path.
- **AND** the engine should successfully synthesize Chinese text without crashing.
