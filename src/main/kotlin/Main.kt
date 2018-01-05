
fun main(args: Array<String>) {
//    val nickbot = Thread(VoteBot.BotRoot("Mjc0MjA4NDI3NDg2NzQwNDgx.C2uwGA.ns88XElNyUjMOhEswnOKBCS6Zjg"))
//
//    nickbot.run()
//
//    println("Started VoteBot...")
//
//    nickbot.join()
//
//    println("He is dead Senor...")

    val rolebot = Thread(BotRoot("Mjc0MjA4NDI3NDg2NzQwNDgx.C2uwGA.ns88XElNyUjMOhEswnOKBCS6Zjg"))

    rolebot.run()

    println("Started Rolebot...")

    rolebot.join()

    println("He is dead Senor...")
}