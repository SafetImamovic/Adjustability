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

skinparam linetype ortho

class Adjustability {
  + MODID: String
  - LOGGER: Logger
}

package enchantments_logic {
  class AnvilHandler {
  }

}

package gui_elements {
  class Button {
    - height: int
    - bgColor: int
    - text: String
    - width: int
    - y: int
    - BORDER_COLOR_BOTTOM: int
    - x: int
    - BORDER_COLOR_TOP: int
  }
}

class Config {
  - BUILDER: Builder
  ~ SPEC: ForgeConfigSpec
}
class CustomTooltip {
  - arraySet: boolean
  ~ MOUSE_OFFSET: int
  ~ PADDING: int
  ~ BORDER_COLOR_TOP: int
  - Buttons: List<Button>
  ~ BACKGROUND_COLOR: int
  ~ BORDER_COLOR_BOTTOM: int
  ~ BLENDED_COLOR: int
  - clickActive: boolean
  ~ x: int
  ~ y: int
  ~ rendering: boolean
}

package render_core {
  class RenderCore {
  }
}

Adjustability  --->  AnvilHandler  : <<create>>
Adjustability  --->  CustomTooltip : <<create>>
AnvilHandler   --->  Adjustability 
Button         --->  RenderCore    
Config         --->  Adjustability 
CustomTooltip  --->  Adjustability 
CustomTooltip  --->  Button        : <<create>>
CustomTooltip "1" *---> "Buttons\n*" Button        
CustomTooltip  --->  RenderCore : <<uses>>
Button  --->  RenderCore : <<uses>>
@enduml
```