package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.data.model.BalanceType
import com.example.data.model.TransactionType
import com.example.ui.components.BusinessSwitcherModal
import com.example.ui.screens.auth.AuthScreen
import com.example.ui.screens.customer.AddCustomerDialog
import com.example.ui.screens.customer.CustomerLedgerScreen
import com.example.ui.screens.customer.CustomersScreen
import com.example.ui.screens.dashboard.DashboardScreen
import com.example.ui.screens.reports.ReportsScreen
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.screens.transaction.AddTransactionScreen
import com.example.ui.theme.*
import com.example.ui.viewmodel.LedgerViewModel
import kotlinx.coroutines.flow.collectLatest

sealed class Screen(val route: String, val title: String, val icon: ImageVector, val outlinedIcon: ImageVector) {
    object Dashboard : Screen("dashboard", "Home", Icons.Filled.Dashboard, Icons.Outlined.Dashboard)
    object Customers : Screen("customers", "Customers", Icons.Filled.People, Icons.Outlined.People)
    object Reports : Screen("reports", "Reports", Icons.Filled.Assessment, Icons.Outlined.Assessment)
    object Settings : Screen("settings", "Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
}

class MainActivity : ComponentActivity() {
    private val viewModel: LedgerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                val businesses by viewModel.businesses.collectAsStateWithLifecycle()
                val activeBizId by viewModel.activeBusinessId.collectAsStateWithLifecycle()

                var showBusinessSwitcher by remember { mutableStateOf(false) }
                var showAddCustomerDialog by remember { mutableStateOf(false) }

                LaunchedEffect(Unit) {
                    viewModel.uiEvent.collectLatest { msg ->
                        Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show()
                    }
                }

                val bottomNavScreens = listOf(
                    Screen.Dashboard,
                    Screen.Customers,
                    Screen.Reports,
                    Screen.Settings
                )

                val showBottomNav = currentRoute in bottomNavScreens.map { it.route }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        if (showBottomNav) {
                            Surface(
                                color = Color.White,
                                shadowElevation = 8.dp,
                                border = BorderStroke(1.dp, BorderSlate100)
                            ) {
                                NavigationBar(
                                    containerColor = Color.White,
                                    tonalElevation = 0.dp,
                                    modifier = Modifier
                                        .testTag("main_bottom_nav")
                                        .navigationBarsPadding()
                                ) {
                                    bottomNavScreens.forEach { screen ->
                                        val isSelected = currentRoute == screen.route
                                        NavigationBarItem(
                                            icon = {
                                                Icon(
                                                    imageVector = if (isSelected) screen.icon else screen.outlinedIcon,
                                                    contentDescription = screen.title
                                                )
                                            },
                                            label = {
                                                Text(
                                                    text = screen.title,
                                                    fontSize = 10.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                                )
                                            },
                                            selected = isSelected,
                                            onClick = {
                                                if (currentRoute != screen.route) {
                                                    navController.navigate(screen.route) {
                                                        popUpTo(navController.graph.findStartDestination().id) {
                                                            saveState = true
                                                        }
                                                        launchSingleTop = true
                                                        restoreState = true
                                                    }
                                                }
                                            },
                                            colors = NavigationBarItemDefaults.colors(
                                                selectedIconColor = Indigo600,
                                                selectedTextColor = Indigo600,
                                                unselectedIconColor = Slate400,
                                                unselectedTextColor = Slate400,
                                                indicatorColor = Indigo50
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = Screen.Dashboard.route,
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        // 1. Dashboard
                        composable(Screen.Dashboard.route) {
                            DashboardScreen(
                                viewModel = viewModel,
                                onNavigateToAddTransaction = { customerId, type ->
                                    val custParam = customerId ?: ""
                                    navController.navigate("add_transaction?customerId=$custParam&type=${type.name}")
                                },
                                onNavigateToAddCustomer = { showAddCustomerDialog = true },
                                onNavigateToCustomerLedger = { customerId ->
                                    navController.navigate("customer_ledger/$customerId")
                                },
                                onNavigateToReports = {
                                    navController.navigate(Screen.Reports.route)
                                },
                                onOpenBusinessSwitcher = { showBusinessSwitcher = true }
                            )
                        }

                        // 2. Customers List
                        composable(Screen.Customers.route) {
                            CustomersScreen(
                                viewModel = viewModel,
                                onNavigateToCustomerLedger = { customerId ->
                                    navController.navigate("customer_ledger/$customerId")
                                },
                                onNavigateToAddTransaction = { customerId, type ->
                                    navController.navigate("add_transaction?customerId=$customerId&type=${type.name}")
                                },
                                onOpenAddCustomerDialog = { showAddCustomerDialog = true }
                            )
                        }

                        // 3. Customer Ledger
                        composable(
                            route = "customer_ledger/{customerId}",
                            arguments = listOf(navArgument("customerId") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val customerId = backStackEntry.arguments?.getString("customerId") ?: ""
                            CustomerLedgerScreen(
                                customerId = customerId,
                                viewModel = viewModel,
                                onNavigateBack = { navController.popBackStack() },
                                onNavigateToAddTransaction = { custId, type ->
                                    navController.navigate("add_transaction?customerId=$custId&type=${type.name}")
                                }
                            )
                        }

                        // 4. Add Transaction Entry
                        composable(
                            route = "add_transaction?customerId={customerId}&type={type}",
                            arguments = listOf(
                                navArgument("customerId") {
                                    type = NavType.StringType
                                    defaultValue = ""
                                },
                                navArgument("type") {
                                    type = NavType.StringType
                                    defaultValue = TransactionType.DEBIT.name
                                }
                            )
                        ) { backStackEntry ->
                            val customerId = backStackEntry.arguments?.getString("customerId")?.takeIf { it.isNotEmpty() }
                            val typeStr = backStackEntry.arguments?.getString("type") ?: TransactionType.DEBIT.name
                            val type = try {
                                TransactionType.valueOf(typeStr)
                            } catch (e: Exception) {
                                TransactionType.DEBIT
                            }

                            AddTransactionScreen(
                                initialCustomerId = customerId,
                                initialType = type,
                                viewModel = viewModel,
                                onNavigateBack = { navController.popBackStack() },
                                onNavigateToLedger = { custId ->
                                    navController.popBackStack()
                                    navController.navigate("customer_ledger/$custId")
                                }
                            )
                        }

                        // 5. Reports
                        composable(Screen.Reports.route) {
                            ReportsScreen(
                                viewModel = viewModel,
                                onNavigateToCustomerLedger = { customerId ->
                                    navController.navigate("customer_ledger/$customerId")
                                }
                            )
                        }

                        // 6. Settings
                        composable(Screen.Settings.route) {
                            SettingsScreen(
                                viewModel = viewModel,
                                onOpenBusinessSwitcher = { showBusinessSwitcher = true },
                                onNavigateToAuth = { navController.navigate("auth") }
                            )
                        }

                        // 7. Auth
                        composable("auth") {
                            AuthScreen(
                                viewModel = viewModel,
                                onAuthSuccess = { navController.popBackStack() }
                            )
                        }
                    }
                }

                // Global Bottom Sheet for Business Switcher
                if (showBusinessSwitcher) {
                    BusinessSwitcherModal(
                        businesses = businesses,
                        activeBusinessId = activeBizId,
                        onSelectBusiness = { bizId ->
                            viewModel.switchBusiness(bizId)
                        },
                        onAddNewBusiness = {
                            navController.navigate(Screen.Settings.route)
                        },
                        onDismiss = { showBusinessSwitcher = false }
                    )
                }

                // Global Add Customer Dialog
                if (showAddCustomerDialog) {
                    AddCustomerDialog(
                        onDismiss = { showAddCustomerDialog = false },
                        onConfirm = { name, mobile, altMobile, address, openingBal, balType, notes ->
                            viewModel.addCustomer(name, mobile, altMobile, address, openingBal, balType, notes)
                            showAddCustomerDialog = false
                        }
                    )
                }
            }
        }
    }
}
