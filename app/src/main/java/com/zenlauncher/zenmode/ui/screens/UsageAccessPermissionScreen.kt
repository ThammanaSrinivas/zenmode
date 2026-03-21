package com.zenlauncher.zenmode.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.zenlauncher.zenmode.R
import com.zenlauncher.zenmode.ui.components.OnboardingScreenLayout
import com.zenlauncher.zenmode.ui.theme.Grey600
import com.zenlauncher.zenmode.ui.theme.Grey800
import com.zenlauncher.zenmode.ui.theme.White
import com.zenlauncher.zenmode.ui.theme.ZenBase
import com.zenlauncher.zenmode.ui.theme.ZenGlow

@Composable
fun UsageAccessPermissionScreen(
    onGrantAccessClick: () -> Unit
) {
    OnboardingScreenLayout(
        progress = 0.5f,
        progressText = "50%",
        buttonText = "Grant access",
        onButtonClick = onGrantAccessClick,
        bottomFooter = null
    ) {
        Spacer(modifier = Modifier.height(20.dp))
        
        // App Icon (Show Zenmode Logo at top center)
        Image(
            painter = painterResource(id = R.drawable.logo),
            contentDescription = "Zenmode Logo",
            modifier = Modifier.size(60.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))

        // Center visual component natively built mimicking the Figma spec "usage_access_permission_overlay"
        Box(
            modifier = Modifier
                .padding(horizontal = 32.dp)
                .fillMaxWidth()
                .background(Grey800.copy(alpha = 0.8f), RoundedCornerShape(10.dp))
                .border(1.dp, Grey800, RoundedCornerShape(10.dp))
                .padding(20.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Usage Access Permission",
                    color = White,
                    style = MaterialTheme.typography.titleMedium
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Inner permission mock
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(com.zenlauncher.zenmode.ui.theme.Black, RoundedCornerShape(8.dp))
                        .border(1.dp, Grey600.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(vertical = 12.dp, horizontal = 16.dp)
                ) {
                    // Row 1: App row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Green icon
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(ZenGlow.copy(alpha = 0.2f))
                                .border(2.dp, ZenGlow, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(ZenBase)
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(12.dp))
                        
                        Text(
                            text = "Zenmode",
                            color = White,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        
                        Spacer(modifier = Modifier.weight(1f))
                        
                        // Fake chevron (using a simple text > for visual approximation)
                        Text(text = ">", color = Grey600)
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Grey600.copy(alpha = 0.5f)))
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Row 2: Toggle row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Toggle knob mock
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(White, RoundedCornerShape(6.dp))
                        )
                        
                        Spacer(modifier = Modifier.width(12.dp))
                        
                        // Line mock
                        Box(
                            modifier = Modifier
                                .width(80.dp)
                                .height(12.dp)
                                .background(White.copy(alpha = 0.8f), RoundedCornerShape(6.dp))
                        )
                        
                        Spacer(modifier = Modifier.weight(1f))
                        Text(text = ">", color = Grey600)
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Text instructions
        Text(
            text = "We want to see which apps are eating your time. Not to judge just to help you reclaim it.",
            color = White,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(horizontal = 40.dp)
        )
        
        Spacer(modifier = Modifier.height(40.dp))
    }
}
