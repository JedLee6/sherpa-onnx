## MODIFIED Requirements

### Requirement: Matcha Model Availability
The matcha-icefall-zh-baker model must be selectable and functional.

#### Scenario: Select Matcha model
- **WHEN** the user opens the model dropdown.
- **THEN** "zh-baker (Matcha)" should appear in the list.

#### Scenario: Synthesize with Matcha model
- **WHEN** the user selects the Matcha model and enters Chinese text.
- **THEN** the engine should synthesize speech without crashing.
