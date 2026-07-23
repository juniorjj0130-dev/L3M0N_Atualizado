-- CreateTable
CREATE TABLE "ClientDataEntry" (
    "id" SERIAL NOT NULL,
    "clientId" TEXT NOT NULL,
    "kind" TEXT NOT NULL,
    "payload" JSONB NOT NULL,
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updatedAt" TIMESTAMP(3) NOT NULL,

    CONSTRAINT "ClientDataEntry_pkey" PRIMARY KEY ("id")
);

-- CreateIndex
CREATE INDEX "ClientDataEntry_clientId_kind_idx" ON "ClientDataEntry"("clientId", "kind");
