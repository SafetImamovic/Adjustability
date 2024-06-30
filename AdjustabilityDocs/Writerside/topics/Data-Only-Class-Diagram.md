# Data Only Class Diagram

<chapter collapsible="true" title="Access Modifier Notation">
<p>Characters that define the visibility:</p>
<table>
<tr><th>Character</th><th>Icon for field</th><th>Icon for method</th><th>Visibility</th></tr>
<tr><td><code>-</code></td><td>&#9723;</td><td>&#9724;</td><td><code>private</code></td></tr>
<tr><td><code>#</code></td><td>&#9671;</td><td>&#9670;</td><td><code>protected</code></td></tr>
<tr><td><code>~</code></td><td>&#9651;</td><td>&#9650;</td><td><code>package private</code></td></tr>
<tr><td><code>+</code></td><td>&#9675;</td><td>&#9679;</td><td><code>public</code></td></tr>
</table>
</chapter>

```plantuml
@startuml
top to bottom direction
package adjustability
{
class Adjustability {
  - LOGGER: Logger
  + MODID: String
}
package enchantments_logic
{
class AnvilHandler
}
class Config {
  - BUILDER: Builder
  ~ SPEC: ForgeConfigSpec
}
package render_core
{
class RenderCore
}
  package gui_elements{
class Button {
  - x: int
  - width: int
  - text: String
  - height: int
  - y: int
  - bgColor: int
}
}
class CustomTooltip {
  ~ BACKGROUND_COLOR: int
  ~ PADDING: int
  ~ MOUSE_OFFSET: int
  ~ x: int
  ~ BLENDED_COLOR: int
  ~ BORDER_COLOR_TOP: int
  ~ y: int
  ~ BORDER_COLOR_BOTTOM: int
  ~ rendering: boolean
}
}
Adjustability  -[#595959,dashed]->  AnvilHandler  : "«create»"
Adjustability  -[#595959,dashed]->  CustomTooltip : "«create»"
AnvilHandler   -[#595959,dashed]->  Adjustability 
Config         -[#595959,dashed]->  Adjustability 
CustomTooltip  -[#595959,dashed]->  Adjustability 
CustomTooltip --> RenderCore : <<uses>>
CustomTooltip --[#595959,dashed]-> Button : "«create»"
Button --> RenderCore : <<uses>>
@enduml
```