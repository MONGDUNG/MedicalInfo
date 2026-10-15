using System;
using Microsoft.EntityFrameworkCore.Migrations;

#nullable disable

namespace MedicalInfo.Web.Migrations
{
    /// <inheritdoc />
    public partial class MemberAdministration : Migration
    {
        /// <inheritdoc />
        protected override void Up(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.AddColumn<DateTime>(
                name: "SuspendedUntil",
                table: "AspNetUsers",
                type: "TEXT",
                nullable: true);

            migrationBuilder.AddColumn<long>(
                name: "TierId",
                table: "AspNetUsers",
                type: "INTEGER",
                nullable: true);

            migrationBuilder.CreateTable(
                name: "AdminAudits",
                columns: table => new
                {
                    Id = table.Column<long>(type: "INTEGER", nullable: false)
                        .Annotation("Sqlite:Autoincrement", true),
                    CreatedAt = table.Column<DateTime>(type: "TEXT", nullable: false),
                    ActorId = table.Column<string>(type: "TEXT", nullable: false),
                    TargetId = table.Column<string>(type: "TEXT", nullable: false),
                    Action = table.Column<string>(type: "TEXT", nullable: false),
                    Detail = table.Column<string>(type: "TEXT", nullable: false),
                    Reason = table.Column<string>(type: "TEXT", nullable: false)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_AdminAudits", x => x.Id);
                });

            migrationBuilder.CreateTable(
                name: "MemberTiers",
                columns: table => new
                {
                    Id = table.Column<long>(type: "INTEGER", nullable: false)
                        .Annotation("Sqlite:Autoincrement", true),
                    Code = table.Column<string>(type: "TEXT", nullable: false),
                    Name = table.Column<string>(type: "TEXT", nullable: false)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_MemberTiers", x => x.Id);
                });

            migrationBuilder.CreateIndex(
                name: "IX_AspNetUsers_TierId",
                table: "AspNetUsers",
                column: "TierId");

            migrationBuilder.CreateIndex(
                name: "IX_MemberTiers_Code",
                table: "MemberTiers",
                column: "Code",
                unique: true);

            migrationBuilder.AddForeignKey(
                name: "FK_AspNetUsers_MemberTiers_TierId",
                table: "AspNetUsers",
                column: "TierId",
                principalTable: "MemberTiers",
                principalColumn: "Id",
                onDelete: ReferentialAction.Restrict);
        }

        /// <inheritdoc />
        protected override void Down(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.DropForeignKey(
                name: "FK_AspNetUsers_MemberTiers_TierId",
                table: "AspNetUsers");

            migrationBuilder.DropTable(
                name: "AdminAudits");

            migrationBuilder.DropTable(
                name: "MemberTiers");

            migrationBuilder.DropIndex(
                name: "IX_AspNetUsers_TierId",
                table: "AspNetUsers");

            migrationBuilder.DropColumn(
                name: "SuspendedUntil",
                table: "AspNetUsers");

            migrationBuilder.DropColumn(
                name: "TierId",
                table: "AspNetUsers");
        }
    }
}
