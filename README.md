# SupaCompose

A framework for building android & desktop apps with Supabase (can be used for other purposes)

Newest version: [![Maven Central](https://img.shields.io/maven-central/v/io.github.jan-tennert.supacompose/Supacompose)](https://search.maven.org/search?q=g%3Aio.github.jan-tennert.supacompose)

# Installation

```kotlin
dependencies {
    implementation("io.github.jan-tennert.supacompose:Supacompose-[module e.g. Auth or Functions]:VERSION")
}
```

# Creating a client

To create a client simply call the createClient top level function:

```kotlin
val client = createSupabaseClient {
    supabaseUrl = System.getenv("SUPABASE_URL") //without https:// !
    supabaseKey = System.getenv("SUPABASE_KEY")

    install(Auth) {
        //on desktop, you have to set the session file. On android and web it's managed by the plugin
        sessionFile = File("C:\\Users\\user\\AppData\\Local\\SupaCompose\\usersession.json")
    }
    //install other plugins
    install(Postgrest)
    install(Storage)
}
```

# Features

#### Core

<details><summary>Creating a custom plugin</summary>

```kotlin
class MyPlugin(private val config: MyPlugin.Config): SupacomposePlugin {

    fun doSomethingCool() {
        println("something cool")
    }
    
    data class Config(var someSetting: Boolean = false)

    companion object : SupacomposePluginProvider<Config, MyPlugin> {

        override val key = "myplugin" //this key is used to identify the plugin when retrieving it

        override fun createConfig(init: Config.() -> Unit): Config {
            //used to create the configuration object for the plugin
            return Config().apply(init)
        }

        override fun setup(builder: SupabaseClientBuilder, config: Config) {
            //modify the supabase client builder
        }

        override fun create(supabaseClient: SupabaseClient, config: Config): MyPlugin {
            //modify the supabase client and return the final plugin instance
            return MyPlugin(config)
        }

    }

}

//make an easy extension for accessing the plugin
val SupabaseClient.myplugin get() = pluginManager.getPlugin<MyPlugin>("myplugin")

//then install it:
val client = createSupabaseClient {
    install(MyPlugin) {
        someSetting = true
    }
}
```

</details>

<details><summary>Initialize the logger</summary>
If you want so see logs for supacompose you have to initialize the logger:

```kotlin
Napier.base(DebugAntilog())
```
</details>

#### Authentication

<details><summary>Feature table</summary>


|         | Login                                            | Signup                                           | Verifying (Signup, Password Reset, Invite) | Logout | Otp |
|---------|--------------------------------------------------|--------------------------------------------------|--------------------------------------------|--------|-----|
| Desktop | phone, password, oauth2 via callback http server | phone, password, oauth2 via callback http server | only with token                            | ✅      | ❌   |
| Android | phone, password, oauth2 via deeplinks            | phone, password, oauth2 via deeplinks            | token, url via deeplinks                   | ✅      | ✅   |
| Web     | phone, password, oauth2                          | phone, password, oauth2                          | token, url                                 | ✅      | ✅   |

❌ = will not be implemented \
✅ = implemented

Session saving: ✅

</details>

<details><summary>Authentication with Desktop</summary>
<p>

<b> To add OAuth support, add this link to the redirect urls in supabase </b>

![img.png](.github/images/desktop_supabase.png)

```kotlin
suspend fun main() {
    val client = createSupabaseClient {
        supabaseUrl = System.getenv("SUPABASE_URL")
        supabaseKey = System.getenv("SUPABASE_KEY")

        install(Auth)
    }
    application {
        Window(::exitApplication) {
            val session by client.auth.currentSession.collectAsState()
            val scope = rememberCoroutineScope()
            if (session != null) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text("Logged in as ${session?.user?.email}")
                }
            } else {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    var email by remember { mutableStateOf("") }
                    var password by remember { mutableStateOf("") }
                    Column {
                        TextField(email, { email = it }, placeholder = { Text("Email") })
                        TextField(
                            password,
                            { password = it },
                            placeholder = { Text("Password") },
                            visualTransformation = PasswordVisualTransformation()
                        )
                        Button(onClick = {
                            scope.launch {
                                client.auth.signUpWith(Email) {
                                    this.email = email
                                    this.password = password
                                }
                            }
                        }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                            Text("Login")
                        }
                        Button(
                            {
                                scope.launch {
                                    client.auth.loginWith(Discord) {
                                        onFail = {
                                            when (it) {
                                                is OAuthFail.Timeout -> {
                                                    println("Timeout")
                                                }
                                                is OAuthFail.Error -> {
                                                    //log error
                                                }
                                            }
                                        }
                                        timeout = 50.seconds
                                        htmlTitle = "SupaCompose"
                                        htmlText = "Logged in. You may continue in the app."
                                    }
                                }
                            },
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        ) {
                            Icon(painterResource("discord_icon.svg"), "", modifier = Modifier.size(25.dp))
                            Text("Log in with Discord")
                        }
                    }
                }

            }
        }
    }

}
```

</p>
</details>

<details><summary>Authentication with Android</summary>

<p>
 <b> When you set the deep link scheme and host in the supabase deeplink plugin and in the android manifest you have to remember to set the additional redirect url in the subabase auth settings. E.g. if you have supacompose as your scheme and login as your host set this to the additional redirect url: </b>

![img.png](.github/images/img.png)
</p>

<blockquote>

<details><summary>MainActivity</summary>
<p>
<b> Note: you should probably use a viewmodel for suspending functions from the SupaCompose library </b>
</p>

<p>

```kotlin
class MainActivity : AppCompatActivity() {

    val supabaseClient = createSupabaseClient {

        supabaseUrl = "your supabase url"
        supabaseKey = "your supabase key"

        install(Auth) {
            scheme = "supacompose"
            host = "login"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initializeAndroid(supabaseClient) //if you don't call this function the library will throw an error when trying to authenticate with oauth
        setContent {
            MaterialTheme {
                val session by supabaseClient.auth.currentSession.collectAsState()
                println(session)
                val scope = rememberCoroutineScope()
                if (session != null) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Text("Logged in as ${session?.user?.email}")
                    }
                } else {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        var email by remember { mutableStateOf("") }
                        var password by remember { mutableStateOf("") }
                        Column {
                            TextField(email, { email = it }, placeholder = { Text("Email") })
                            TextField(
                                password,
                                { password = it },
                                placeholder = { Text("Password") },
                                visualTransformation = PasswordVisualTransformation()
                            )
                            Button(onClick = {
                                scope.launch {
                                    supabaseClient.auth.loginWith(Email) {
                                        this.email = email
                                        this.password = password
                                    }
                                }
                            }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                                Text("Login")
                            }
                            Button(
                                {
                                    scope.launch {
                                        client.auth.loginWith(Discord)
                                    }
                                },
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            ) {
                                Icon(painterResource("discord_icon.svg"), "", modifier = Modifier.size(25.dp))
                                Text("Log in with Discord")
                            }
                        }
                    }
                }
            }
        }
    }

}
```

</p>
</details>

<details><summary>AndroidManifest</summary>
<p>

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="io.github.jan.supacompose.android">

    <uses-permission android:name="android.permission.INTERNET"/>

    <application
            android:allowBackup="false"
            android:supportsRtl="true"
            android:theme="@style/Theme.AppCompat.Light.NoActionBar">
        <activity android:name=".MainActivity" android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN"/>
                <category android:name="android.intent.category.LAUNCHER"/>
                <action android:name="android.intent.action.VIEW"/>
                <category android:name="android.intent.category.DEFAULT"/>
                <category android:name="android.intent.category.BROWSABLE"/>
                <!-- This is important for deeplinks. -->
                <data android:scheme="supacompose"
                      android:host="login"/>
            </intent-filter>
        </activity>
    </application>
</manifest>
```

</p>
</details>

</blockquote>

</details>

<details><summary>Authentication with Web</summary>

<p>

```kotlin
val client = createSupabaseClient {
    supabaseUrl = ""
    supabaseKey = ""

    install(Auth)
}
client.auth.initializeWeb()

renderComposable(rootElementId = "root") {
    val session by client.auth.currentSession.collectAsState()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    if(session != null) {
        Span({ style { padding(15.px) } }) {
            Text("Logged in as ${session!!.user?.email}")
        }
    } else {
        EmailInput(email) {
            onInput {
                email = it.value
            }
        }
        PasswordInput(password) {
            onInput {
                password = it.value
            }
        }
        Button({
            onClick {
                scope.launch {
                    client.auth.loginWith(Email) {
                        this.email = email
                        this.password = password
                    }
                }
            }
        }) {
            Text("Login")
        }
        Button({
            onClick {
                scope.launch {
                    client.auth.loginWith(Discord)
                }
            }
        }) {
            Text("Login with Discord")
        }
    }
}
```

</p>

</details>

<details><summary>Manage Users (Server Only)</summary>

**For admin methods you need the service role secret which you should never share with anyone nor include it with your app.**

<p>

```kotlin
client.auth.importAuthToken("service role secret") //also disable both autoLoadFromStorage and autoRefresh in the auth config

client.auth.retrieveUsers() //get all signed in users
//register users
client.auth.admin.createUserWithEmail {
    email = "example@foo.bar"
    password = "12345678"
    autoConfirm = true //automatically confirm this email address
}
client.auth.admin.createUserWithPhone {
    phoneNumber = "123456789"
    password = "12345678"
}
//update user
client.auth.admin.updateUserById("uid") {
    phone = "12345679"
    phoneConfirm = true
}
//generate link
val (link, user) = client.auth.admin.generateLinkFor(LinkType.MagicLink) {
    email = "example@foo.bar"
}
val (link, user) = client.auth.admin.generateLinkFor(LinkType.Signup) {
    email = "example@foo.bar"
    password = "12345678"
}
```

</p>

</details>

#### Database/Postgres

<details><summary>Make database calls</summary>

```kotlin
//a data class for a message

data class Message(val text: String, @SerialName("author_id") val authorId: String, val id: Int)

```

<b>If you use the syntax with property references the client will automatically look for @SerialName annotiations on your class property and if it has one it will use the value as the column name. (Only JVM)</b>

<blockquote>

<details><summary>Select</summary>

```kotlin
client.postgrest["messages"]
    .select {
        //you can use that syntax
        Message::authorId eq "someid"
        Message::text neq "This is a text!"
        Message::authorId isIn listOf("test", "test2")

        //or this. But they are the same
        eq("author_id", "someid")
        neq("text", "This is a text!")
        isIn("author_id", listOf("test", "test2"))
    }
````

</details>

<details><summary>Insert</summary>

```kotlin
client.postgrest["messages"]
    .insert(Message("This is a text!", "someid", 1))
````

</details>

<details><summary>Update</summary>

```kotlin
client.postgrest["messages"]
    .update(
        {
            Message::text setTo "This is the edited text!"
        }
    ) {
        Message::id eq 2
    }
````

</details>

<details><summary>Delete</summary>

```kotlin
client.postgrest["messages"]
    .delete {
        Message::id eq 2
    }
````

</details>

</blockquote>

</details>

#### Storage

<details><summary>Managing buckets</summary>

```kotlin
//create a bucket
client.storage.createBucket(name = "images", id = "images", public = false)

//empty bucket
client.storage.emptyBucket(id = "images")

//and so on
```

</details>

<details><summary>Uploading files</summary>

```kotlin
val bucket = client.storage["images"]

//upload a file (jvm)
bucket.upload("landscape.png", File("landscape.png"))

//download a file (jvm)
bucket.downloadTo("landscape.png", File("landscape.png"))

//copy a file

bucket.copy("landscape.png", "landscape2.png")

//and so on
```

</details>

#### Realtime

<details><summary>Creating/Joining the channel</summary>

```kotlin
val channel = supabaseClient.realtime.createChannel("#random") {

    presence {
        //presence options
    }

    broadcast {
        //broadcast options
    }

}
channel.postgresChangeFlow<PostgresAction.Insert> {} // listen for changes
//listen for broadcasts ...

//in the end join the channel
channel.join()
```

</details>
<details><summary>Listening for database changes</summary>

```kotlin
val changeFlow: Flow<PostgresAction.Insert> = channel.postgresChangeFlow<PostgresAction.Insert> {
    schema = "public"
    table = "test"
}
```

</details>
<details><summary>Broadcast API</summary>

```kotlin
val broadcastFlow: Flow<Position> = channel.broadcastFlow("position") //under the event "position"

channel.broadcast("position", Position(20, 30)) //broadcast your position to other clients (in the event "position")
```

</details>
<details><summary>Presence API</summary>

```kotlin
@Serializable
data class PresenceData(val userId: Int, val username: String)

channel.presenceFlow()
    .onEach {
        //joins and leaves are a map. The keys are either an id generated by realtime or a custom key which can be customized in the client builder. 
        otherUsers += it.decodeJoinsAs<PresenceData>()
        otherUsers -= it.decodeLeavesAs<PresenceData>()
    }
    .launchIn(SomeScope)
channel.join()
channel.track(PresenceData(2, "Example")) //send your "state"/presence to other clients
```

</details>

#### Functions (Edge Functions)
<details><summary>Execute edge functions directly</summary>

```kotlin
@Serializable
data class SomeData(val name: String)

val response: HttpResponse = client.functions("test")
//with body
val response: HttpResponse = client.functions(
    function = "test",
    body = SomeData("Name")
    headers = Headers.build {
        append(HttpHeaders.ContentType, "application/json")
    }
)
```
</details>
<details><summary>Store your edge function in a variable</summary>

```kotlin
@Serializable
data class SomeData(val name: String)

val testFunction: EdgeFunction = client.functions.buildEdgeFunction {
    functionName = "test"
    headers.append(HttpHeaders.ContentType, "application/json")
}

val response: HttpResponse = testFunction()
//with body
val response: HttpResponse = testFunction(SomeData("Name"))
```
</details>


# Credits 

- Postgres Syntax inspired by https://github.com/supabase-community/postgrest-kt
- Plugin system inspired by ktor