package com.zenlauncher.zenmode

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import android.app.AlertDialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.TimeoutCancellationException
import com.zenlauncher.zenmode.coreapi.UsageRepository
import com.zenlauncher.zenmode.coreapi.services.ServiceLocator

class BuddyInviteFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.layout_widget_buddy_invite, container, false)
        
        view.findViewById<Button>(R.id.btn_invite_buddy)?.setOnClickListener {
             showAddBuddyDialog()
        }
        
        return view
    }

    private fun showAddBuddyDialog() {
        context?.let { ctx ->
            val dialogView = LayoutInflater.from(ctx).inflate(R.layout.dialog_add_buddy, null)
            val dialog = AlertDialog.Builder(ctx)
                .setView(dialogView)
                .create()
            
            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            
            // Populate UID
            val analyticsManager = ServiceLocator.analyticsManager
            val repository = UsageRepository(ctx, analyticsManager)
            val currentUserId = ServiceLocator.authProvider.getCurrentUserId()
            val uid = repository.getUserUid() ?: currentUserId
            
            val tvMyCode = dialogView.findViewById<android.widget.TextView>(R.id.tv_my_code_value)
            if (uid != null && uid.length > 7) {
                tvMyCode.text = "${uid.take(7)}..."
            } else {
                tvMyCode.text = uid ?: "Not Signed In"
            }

            // Copy to clipboard logic
            dialogView.findViewById<View>(R.id.iv_copy_code)?.setOnClickListener {
                uid?.let { code ->
                    val clipboard = ctx.getSystemService(android.content.ClipboardManager::class.java)
                    val clip = android.content.ClipData.newPlainText("ZenMode Code", code)
                    clipboard.setPrimaryClip(clip)
                    // Analytics
                    ServiceLocator.analyticsTracker.trackBuddyCodeGenerated("copy_link")
                    android.widget.Toast.makeText(ctx, "Code copied!", android.widget.Toast.LENGTH_SHORT).show()
                }
            }

            // Add Buddy Logic
            val etBuddyCode = dialogView.findViewById<android.widget.EditText>(R.id.et_buddy_code)
            val btnAddBuddy = dialogView.findViewById<View>(R.id.btn_add_buddy)
            
            // Initially disable button
            btnAddBuddy.isEnabled = false
            btnAddBuddy.alpha = 0.5f

            etBuddyCode.addTextChangedListener(object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    val input = s.toString().trim()
                    btnAddBuddy.isEnabled = input.isNotEmpty()
                    btnAddBuddy.alpha = if (input.isNotEmpty()) 1.0f else 0.5f
                }
                override fun afterTextChanged(s: android.text.Editable?) {}
            })

            btnAddBuddy.setOnClickListener {
                val targetUid = etBuddyCode.text.toString().trim()
                
                if (targetUid.isEmpty()) return@setOnClickListener

                if (targetUid == currentUserId) {
                    android.widget.Toast.makeText(ctx, "You cannot add yourself as a buddy.", android.widget.Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                // Check network connectivity first
                val connectivityManager = ctx.getSystemService(android.net.ConnectivityManager::class.java)
                val activeNetwork = connectivityManager?.activeNetwork
                val networkCapabilities = connectivityManager?.getNetworkCapabilities(activeNetwork)
                val isConnected = networkCapabilities?.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET) == true

                if (!isConnected) {
                    android.widget.Toast.makeText(ctx, "No internet connection. Please check your network and try again.", android.widget.Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val firestoreDataSource = ServiceLocator.firestoreDataSource

                // Disable button to prevent double-clicks
                btnAddBuddy.isEnabled = false
                btnAddBuddy.alpha = 0.5f

                // Use coroutines for cleaner async handling
                lifecycleScope.launch {
                    try {
                        // Check if user exists
                        val user = firestoreDataSource.getUser(targetUid)

                        if (user != null) {
                            val buddyName = user.displayName

                            // Check if relationship already exists
                            val myUid = currentUserId
                            val isAlreadyBuddy = myUid?.let { firestoreDataSource.checkRelationshipExists(it, targetUid) } == true

                            if (isAlreadyBuddy) {
                                android.widget.Toast.makeText(ctx, "You are already buddies with $buddyName!", android.widget.Toast.LENGTH_SHORT).show()
                                return@launch
                            }
                            
                            // Create both relationship directions atomically using repository
                            if (myUid != null) {
                                firestoreDataSource.sendBuddyInvite(myUid, targetUid)

                                // Clear cached buddies so main page re-fetches
                                repository.clearCachedBuddy()
                                
                                // Trigger immediate refresh of buddy stats
                                activity?.let { act ->
                                    val viewModel = androidx.lifecycle.ViewModelProvider(act)[MainViewModel::class.java]
                                    viewModel.fetchBuddyData()
                                }

                                // Analytics
                                ServiceLocator.analyticsTracker.trackBuddyLinkAccepted("buddy")

                                android.widget.Toast.makeText(ctx, "Successfully added $buddyName!", android.widget.Toast.LENGTH_SHORT).show()
                                dialog.dismiss()
                            }
                        } else {
                            android.widget.Toast.makeText(ctx, "User ID not found. Please check the ID and try again.", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: TimeoutCancellationException) {
                        android.widget.Toast.makeText(ctx, "Connection timed out. Please check your network and try again.", android.widget.Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        val errorMessage = when {
                            e.message?.contains("offline", ignoreCase = true) == true ->
                                "Unable to connect. Please check your internet and try again."
                            else -> "Search failed: ${e.message}"
                        }
                        android.widget.Toast.makeText(ctx, errorMessage, android.widget.Toast.LENGTH_SHORT).show()
                    } finally {
                        btnAddBuddy.isEnabled = true
                        btnAddBuddy.alpha = 1.0f
                    }
                }
            }

            dialog.show()
        }
    }

    companion object {
        fun newInstance(): BuddyInviteFragment {
            return BuddyInviteFragment()
        }
    }
}
