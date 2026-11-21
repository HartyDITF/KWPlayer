package fyi.kiyori.modernmediaplayer.feature.themes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
@Composable fun ThemesDialog(selected:String, selectedAccent:Color, onSelect:(String)->Unit, onAccent:(Color)->Unit, onDismiss:()->Unit) {
 val themes=listOf(Triple("standard","Modern Dark",Color(0xFF101318)),Triple("retro","Classic Orange",Color(0xFF17130D)),Triple("light","Light",Color(0xFFF2F4F7)),Triple("amoled","AMOLED Black",Color.Black))
 AlertDialog(onDismissRequest=onDismiss,title={Text("Темы и скины")},text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)){
  themes.forEach{(id,name,bg)->Row(Modifier.fillMaxWidth().border(if(selected==id)2.dp else 1.dp,if(selected==id)selectedAccent else Color(0xFF2A2F3A),RoundedCornerShape(10.dp)).background(bg,RoundedCornerShape(10.dp)).clickable{onSelect(id)}.padding(12.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("01:42  ▮▮▮▮▮",color=Color(0xFF5CF2CF),fontSize=12.sp);Text(name,color=if(id=="light")Color.DarkGray else Color.White)};Text(if(selected==id)"✓" else "○",color=selectedAccent)}}
  Text("Акцентный цвет",color=Color(0xFF9BA4B5),fontSize=12.sp)
  Row(horizontalArrangement=Arrangement.spacedBy(10.dp)){listOf(Color(0xFFFF8F1F),Color(0xFF5CF2CF),Color(0xFF62C4FF),Color(0xFFFF62C7),Color(0xFFB69CFF)).forEach{c->Box(Modifier.weight(1f).height(28.dp).background(c,RoundedCornerShape(8.dp)).clickable{onAccent(c)})}}
 }},confirmButton={TextButton(onClick=onDismiss){Text("Готово")}})
}
