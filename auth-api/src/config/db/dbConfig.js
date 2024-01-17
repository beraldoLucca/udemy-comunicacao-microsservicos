import Sequelize from "sequelize";

const sequelize = new Sequelize("auth-db", "admin", "123456", {
    host: "localhost",
    dialect: "postgres",
    port: 5433,
    quoteIdentifiers: false,
    define: {
        syncOnAssociation: true,
        timestamps: false,
        underscored: true,
        underscoredAll: true,
        freezeTableName: true
    }
})

sequelize
.authenticate()
.then(() => {
    console.info('COnnection has been stablished!')
})
.catch((err) =>{
    console.error("Unable to connect to the database")
    console.error(err.message)
})

export default sequelize;