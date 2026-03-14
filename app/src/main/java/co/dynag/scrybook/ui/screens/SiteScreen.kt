package co.dynag.scrybook.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import co.dynag.scrybook.R
import co.dynag.scrybook.data.model.Site
import co.dynag.scrybook.ui.components.ScryBookBottomBar
import co.dynag.scrybook.ui.viewmodel.SiteViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SiteScreen(
    projectPath: String,
    onBack: () -> Unit,
    viewModel: SiteViewModel = hiltViewModel()
) {
    val sites by viewModel.sites.collectAsState()
    val selected by viewModel.selected.collectAsState()

    LaunchedEffect(projectPath) { viewModel.load(projectPath) }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, stringResource(R.string.action_back))
                    }
                },
                title = {
                    Text(stringResource(R.string.nav_sites), fontWeight = FontWeight.Bold)
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        bottomBar = { ScryBookBottomBar() },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.newSite() },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
            }
        }
    ) { padding ->
        if (sites.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Default.Language, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                    Text(stringResource(R.string.sites_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(sites, key = { it.id }) { site ->
                    SiteCard(
                        site = site,
                        onClick = { viewModel.select(site) },
                        onDelete = { viewModel.delete(site.id) }
                    )
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }

    // Edit sheet
    selected?.let { site ->
        SiteEditSheet(
            site = site,
            onSave = { viewModel.save(it) },
            onDismiss = { viewModel.select(null) }
        )
    }
}

@Composable
private fun SiteCard(site: Site, onClick: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        onClick = onClick
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.tertiaryContainer,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Language,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = site.nom.ifBlank { stringResource(R.string.site_unnamed) },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (site.contenu.isNotBlank()) {
                    Text(
                        text = site.contenu.lines().firstOrNull()?.take(80) ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SiteEditSheet(site: Site, onSave: (Site) -> Unit, onDismiss: () -> Unit) {
    var nom by remember(site.id) { mutableStateOf(site.nom) }
    var contenu by remember(site.id) { mutableStateOf(site.contenu) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .imePadding()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                if (site.id == 0L) stringResource(R.string.site_new) else stringResource(R.string.site_edit),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            OutlinedTextField(
                value = nom,
                onValueChange = { nom = it },
                label = { Text(stringResource(R.string.site_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Title, null) }
            )

            OutlinedTextField(
                value = contenu,
                onValueChange = { contenu = it },
                label = { Text(stringResource(R.string.site_content_markdown)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 200.dp),
                minLines = 8,
                placeholder = { Text("## Titre\n\nContenu en **markdown**...") }
            )

            // Aide markdown
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "Aide Markdown",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "**gras** • *italique* • ## Titre • - liste",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Button(
                onClick = {
                    onSave(site.copy(nom = nom, contenu = contenu))
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = nom.isNotBlank()
            ) {
                Text(stringResource(R.string.action_save))
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}
