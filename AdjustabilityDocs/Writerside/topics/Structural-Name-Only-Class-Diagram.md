# Structural/Name Only Class Diagram

```plantuml
@startuml
top to bottom direction

skinparam linetype ortho

class Adjustability

package enchantments_logic {
  class AnvilHandler 
}

package gui_elements {
  class Button
}

class Config
class CustomTooltip

package render_core {
  class RenderCore
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