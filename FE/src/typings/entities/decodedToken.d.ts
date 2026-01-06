namespace Entity {
    interface DecodedToken {
        userId: string
        userCode: string
        fullName: string
        rolesName: string
        rolesCode: string[]
        exp: number
        email: string
        pictureUrl: string
        roleSwitch: string
        roleScreen: string | undefined
    }
}